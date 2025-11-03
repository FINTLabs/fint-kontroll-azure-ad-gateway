package no.fintlabs.kafka;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.config.Config;
import no.fintlabs.config.ConfigGroup;
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
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceGroupConsumerService {
    private final MsGraphGroup msGraphGroup;
    private final Config.KafkaConfig kafkaConfig;
    private final ConfigGroup configGroup;
    private final ConcurrentHashMap<String, Optional<ResourceGroup>> resourceGroupCache = new ConcurrentHashMap<>();
    private Sinks.Many<Tuple2<String, Optional<ResourceGroup>>> resourceGroupSink;

    @PostConstruct
    void init() {
        if (resourceGroupSink == null) {
            resourceGroupSink = Sinks.many().unicast().onBackpressureBuffer();
        }
        subscribeToSink();
    }

    /** Allow tests to inject a mock sink; rewire the subscription. */
    protected void setResourceGroupSink(Sinks.Many<Tuple2<String, Optional<ResourceGroup>>> sink) {
        this.resourceGroupSink = sink;
        subscribeToSink();
    }

    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static final int CONCURRENCY = Math.min(CORES * 8, 256);

    private void subscribeToSink() {
        resourceGroupSink.asFlux()
                .flatMap(kv ->
                                Mono.fromCallable(() -> {
                                            updateAzure(kv.getT1(), kv.getT2());
                                            return 1;
                                        })
                                        .subscribeOn(Schedulers.boundedElastic()),
                        CONCURRENCY,
                        1024
                )
                .onErrorContinue((e, o) -> log.error("Failed to update Azure", e))
                .subscribe();
    }


    @Bean
    public ConcurrentMessageListenerContainer<String, ResourceGroup> ResourceGroupConsumer(
            ParameterizedListenerContainerFactoryService parameterizedListenerContainerFactoryService
    ) {
        TopicNamePrefixParameters topicNamePrefixParameters = TopicNamePrefixParameters.builder()
                .orgIdApplicationDefault()
                .domainContextApplicationDefault()
                .build();

        ListenerConfiguration<ResourceGroup> listenerConfiguration =
                ListenerConfiguration.builder(ResourceGroup.class)
                        .groupIdApplicationDefault()
                        .maxPollRecords(kafkaConfig.getMaxpollrecords())
                        .maxPollInterval(Duration.ofMinutes(kafkaConfig.getMaxpollinterval()))
                        .errorHandler(new DefaultErrorHandler())
                        .continueFromPreviousOffsetOnAssignment()
                        .build();

        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resourceName("resource-group")
                .topicNamePrefixParameters(topicNamePrefixParameters)
                .build();

        var factory = parameterizedListenerContainerFactoryService
                .createBatchListenerContainerFactory(this::processEntityBatch, listenerConfiguration,
                        c -> c.setAutoStartup(true));

        return factory.createContainer(entityTopicNameParameters);
    }

    public void processEntityBatch(List<ConsumerRecord<String, ResourceGroup>> records) {
        synchronized (resourceGroupCache) {
            for (ConsumerRecord<String, ResourceGroup> record : records) {
                String kafkaKey = record.key();
                ResourceGroup resourceGroup = record.value();

                if (resourceGroupCache.containsKey(kafkaKey)) {
                    Optional<ResourceGroup> fromCache = resourceGroupCache.get(kafkaKey);

                    if (fromCache.isEmpty() && resourceGroup == null) {
                        log.debug("Skip processing of entity as cache already contains deleted group on resourceGroupId: {}", kafkaKey);
                        continue;
                    }

                    if (resourceGroup != null && fromCache.isPresent() && resourceGroup.equals(fromCache.get())) {
                        log.debug("Skip entity as it is unchanged: {}", resourceGroup.getResourceName());
                        continue;
                    }
                }

                resourceGroupCache.put(kafkaKey, Optional.ofNullable(resourceGroup));
                resourceGroupSink.tryEmitNext(Tuples.of(kafkaKey, Optional.ofNullable(resourceGroup)));
            }
        }
    }

    void updateAzure(String kafkaKey, Optional<ResourceGroup> resourceGroupOptional) throws Exception {
        String randomUUID = UUID.randomUUID().toString();
        log.debug("Starting updateAzure function {}.", randomUUID);
        ResourceGroup resourceGroup;
        if (resourceGroupOptional.isPresent()) {
            resourceGroup = resourceGroupOptional.get();
            boolean groupexists;
            groupexists = msGraphGroup.doesGroupExist(resourceGroup.getId());
            if (resourceGroup.getResourceName() != null && !groupexists) {
                log.debug("Adding Group to Azure: {}", resourceGroup.getResourceName());
                msGraphGroup.addGroupToAzureAsync(resourceGroup);
            } else {
                if (configGroup.getAllowgroupupdate() && resourceGroup.getIdentityProviderGroupObjectId() != null) {
                    msGraphGroup.updateGroup(resourceGroup);
                    log.info("Updated group with ResourceGroupId {}", resourceGroup.getId());
                } else if (!configGroup.getAllowgroupupdate()) {
                    log.warn("ResourceGroupId {} was NOT updated, as \"allowgroupupdate\" is set to false", resourceGroup.getId());
                }
                else if (resourceGroup.getIdentityProviderGroupObjectId() == null) {
                    log.warn("ResourceGroupId {} was NOT updated, as IdentityProviderGroupObjectId is not present in Kafka message", resourceGroup.getId());
                }
            }
        } else {
            if (configGroup.getAllowgroupdelete()) {
                log.debug("Deleting group from Azure with id '{}'", kafkaKey);
                msGraphGroup.deleteGroupAsync(kafkaKey);
            } else {
                log.warn("ResourceGroupId {} is NOT deleted, as environment parameter allowgroupdelete is set to false", kafkaKey);
            }
        }
        log.debug("Stopping updateAzure function {}.", randomUUID);
    }
}
