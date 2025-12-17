package no.fintlabs.kafka;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.config.Config;
import no.fintlabs.group.MsGraphGroup;
import no.fintlabs.kafka.consuming.ListenerConfiguration;
import no.fintlabs.kafka.consuming.ParameterizedListenerContainerFactoryService;
import no.fintlabs.kafka.topic.name.EntityTopicNameParameters;
import no.fintlabs.kafka.topic.name.TopicNamePrefixParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor

public class ResourceGroupMembershipConsumerService {
    private final MsGraphGroup msGraphGroup;
    private final Config.KafkaConfig kafkaConfig;
    private final ConcurrentHashMap<String, Optional<ResourceGroupMembership>> resourceGroupMembershipCache;
    private final Sinks.Many<Tuple2<String, Optional<ResourceGroupMembership>>> resourceGroupMembershipSink =
            Sinks.many().multicast().onBackpressureBuffer();



    @PostConstruct
    void init() {
        resourceGroupMembershipSink.asFlux()
                .flatMap(t ->
                                Mono.fromRunnable(
                                                () -> updateAzureWithMembership(t.getT1(), t.getT2())
                                        )
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .onErrorResume(e -> {
                                            log.error("Graph update failed key={}", t.getT1(), e);
                                            return Mono.empty();
                                        }),
                        20
                )
                .doOnSubscribe(s -> log.info("ResourceGroupMembership subscribed"))
                .doFinally(sig -> log.error("ResourceGroupMembership terminated signal={}", sig))
                .subscribe();
    }

    public void onMembershipUpdate(String key, Optional<ResourceGroupMembership> membership) {
        resourceGroupMembershipSink.tryEmitNext(reactor.util.function.Tuples.of(key, membership));
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, ResourceGroupMembership> resourceGroupMembershipConsumer(
            ParameterizedListenerContainerFactoryService parameterizedListenerContainerFactoryService
    ) {
        TopicNamePrefixParameters topicNamePrefixParameters = TopicNamePrefixParameters.builder()
                .orgIdApplicationDefault()
                .domainContextApplicationDefault()
                .build();

        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters.builder()
                .resourceName("resource-group-membership")
                .topicNamePrefixParameters(topicNamePrefixParameters)
                .build();

        ListenerConfiguration<ResourceGroupMembership> listenerConfiguration =
                ListenerConfiguration.builder(ResourceGroupMembership.class)
                        .groupIdApplicationDefault()
                        .maxPollRecords(kafkaConfig.getMaxpollrecords())
                        .maxPollInterval(Duration.ofMinutes(5))
                        .errorHandler(new DefaultErrorHandler())
                        .continueFromPreviousOffsetOnAssignment()
                        .build();

        var factory = parameterizedListenerContainerFactoryService
                .createBatchListenerContainerFactory(this::processMembershipBatch, listenerConfiguration,
                        c -> c.setAutoStartup(true));

        return factory.createContainer(entityTopicNameParameters);
    }

    public void processMembershipBatch(List<ConsumerRecord<String, ResourceGroupMembership>> records) {
        // TODO: IS this synchronized necessary?
        synchronized (resourceGroupMembershipCache) {
            for (ConsumerRecord<String, ResourceGroupMembership> record : records) {
                String kafkaKey = record.key();
                ResourceGroupMembership membership = record.value();

                if (kafkaKey == null || (membership != null
                        && (membership.getAzureGroupRef() == null || membership.getAzureUserRef() == null))) {
                    log.error("Error when processing entity. Kafka key or values is null. Unsupported!. ResourceGroupMembership object: {}",
                            (membership != null ? membership : "null"));
                    return;
                }

                log.debug("Processing entity with key: {}", kafkaKey);

                if (resourceGroupMembershipCache.containsKey(kafkaKey)) {
                    Optional<ResourceGroupMembership> fromCache = resourceGroupMembershipCache.get(kafkaKey);

                    if (fromCache.isEmpty() && membership == null) {
                        log.debug("Duplicate delete (tombstone) for membership key={}, will STILL process/emit", kafkaKey);
                    }

                    if (membership != null && fromCache.isPresent() && membership.equals(fromCache.get())) {
                        log.debug("Unchanged membership userID={} groupID={} key={} - will STILL process/emit",
                                membership.getAzureUserRef(),
                                membership.getAzureGroupRef(),
                                kafkaKey);
                    }
                }

                Optional<ResourceGroupMembership> next = Optional.ofNullable(membership);
                if (!resourceGroupMembershipCache.containsKey(kafkaKey)) {
                    resourceGroupMembershipCache.put(kafkaKey, next);
                } else {
                    Optional<ResourceGroupMembership> prev = resourceGroupMembershipCache.get(kafkaKey);
                    if (!next.equals(prev)) {
                        resourceGroupMembershipCache.put(kafkaKey, next);
                    }
                }

                resourceGroupMembershipSink.emitNext(
                        Tuples.of(kafkaKey, next),
                        (st, er) -> er == Sinks.EmitResult.FAIL_OVERFLOW
                                || er == Sinks.EmitResult.FAIL_NON_SERIALIZED
                );

                log.debug("Emit OK key={} (delete={})", kafkaKey, next.isEmpty());
            }
        }
    }

    void updateAzureWithMembership(String kafkaKey, Optional<ResourceGroupMembership> resourceGroupMembership) {
        String randomUUID = UUID.randomUUID().toString();
        log.debug("Starting updateAzureWithMembership function {}.", randomUUID);

        if (resourceGroupMembership.isEmpty()) {
            msGraphGroup.deleteGroupMembership(kafkaKey);
        } else {
            msGraphGroup.addGroupMembership(resourceGroupMembership.get(), kafkaKey);
        }
        log.debug("Stopping updateAzureWithMembership function {}.", randomUUID);
    }

}


