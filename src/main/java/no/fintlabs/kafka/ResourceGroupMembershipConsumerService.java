package no.fintlabs.kafka;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.AzureClient;
import no.fintlabs.Config;
import no.fintlabs.kafka.consuming.ListenerConfiguration;
import no.fintlabs.kafka.consuming.ParameterizedListenerContainerFactoryService;
import no.fintlabs.kafka.topic.name.EntityTopicNameParameters;
import no.fintlabs.kafka.topic.name.TopicNamePrefixParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
@RequiredArgsConstructor

public class ResourceGroupMembershipConsumerService {
    private final AzureClient azureClient;
    private final Config.KafkaConfig kafkaConfig;
    private final ConcurrentHashMap<String, Optional<ResourceGroupMembership>> resourceGroupMembershipCache;
    private final Sinks.Many<Tuple2<String, Optional<ResourceGroupMembership>>> resourceGroupMembershipSink =
            Sinks.many().unicast().onBackpressureBuffer();

    @PostConstruct
    void init() {
        resourceGroupMembershipSink.asFlux()
                .parallel(20)
                .runOn(Schedulers.boundedElastic())
                .subscribe(t -> updateAzureWithMembership(t.getT1(), t.getT2()));
    }

    public void onMembershipUpdate(String key, Optional<ResourceGroupMembership> membership) {
        resourceGroupMembershipSink.tryEmitNext(reactor.util.function.Tuples.of(key, membership));
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, ResourceGroupMembership> ResourceGroupMembershipConsumer(
            ParameterizedListenerContainerFactoryService parameterizedListenerContainerFactoryService
    ) {
        TopicNamePrefixParameters topicNamePrefixParameters = TopicNamePrefixParameters.builder()
                .orgIdApplicationDefault()
                .domainContextApplicationDefault()
                .build();

        ListenerConfiguration listenerConfiguration = ListenerConfiguration.builder()
                .seekingOffsetResetOnAssignment(kafkaConfig.isSeekingOffsetResetOnAssignment())
                .maxPollRecords(kafkaConfig.getMaxpollrecords())
                .build();

        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resourceName("resource-group-membership")
                .topicNamePrefixParameters(topicNamePrefixParameters)
                .build();

        return parameterizedListenerContainerFactoryService.createRecordListenerContainerFactory(
                        ResourceGroupMembership.class,
                        consumerRecord -> processEntity(
                                consumerRecord.value(), consumerRecord.key()),
                        listenerConfiguration)
                .createContainer(entityTopicNameParameters);
    }

    void updateAzureWithMembership(String kafkakKey, Optional<ResourceGroupMembership> resourceGroupMembership) {
        String randomUUID = UUID.randomUUID().toString();
        log.debug("Starting updateAzureWithMembership function {}.", randomUUID);

        if (resourceGroupMembership.isEmpty()) {
            azureClient.deleteGroupMembership(kafkakKey);
        } else {
            azureClient.addGroupMembership(resourceGroupMembership.get(), kafkakKey);
        }
        log.debug("Stopping updateAzureWithMembership function {}.", randomUUID);
    }

    public void processEntity(ResourceGroupMembership resourceGroupMembership, String kafkaKey) {

            if (kafkaKey == null || (resourceGroupMembership != null && (resourceGroupMembership.getAzureGroupRef() == null || resourceGroupMembership.getAzureUserRef() == null))) {
                log.error("Error when processing entity. Kafka key or values is null. Unsupported!. ResourceGroupMembership object: {}",
                        (resourceGroupMembership != null ? resourceGroupMembership : "null"));
                return;
            }

        synchronized (resourceGroupMembershipCache) {
            // Check resourceGroupCache if object is known from before
            log.debug("Processing entity with key: {}", kafkaKey);

            if (resourceGroupMembershipCache.containsKey(kafkaKey)) {
                log.debug("Found key in cache: {}", kafkaKey);

                Optional<ResourceGroupMembership> fromCache = resourceGroupMembershipCache.get(kafkaKey);

                log.debug("From cache: {}", fromCache);

                if (fromCache.isEmpty() && resourceGroupMembership == null) {
                    // resourceGroupMembership is a delete message already in cache
                    log.debug("Skipping processing of already cached delete group membership message: {}", kafkaKey);
                    return;
                }

                if (resourceGroupMembership != null && fromCache.isPresent() && resourceGroupMembership.equals(fromCache.get())) {
                    // New kafka message, but unchanged resourceGroupMembership from last time
                    log.debug("Skipping processing of group membership, as it is unchanged from before: userID: {} groupID {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                    return;
                }
            }
            resourceGroupMembershipCache.put(kafkaKey, Optional.ofNullable(resourceGroupMembership));
            resourceGroupMembershipSink.tryEmitNext(Tuples.of(kafkaKey, Optional.ofNullable(resourceGroupMembership)));
        }
    }
}


