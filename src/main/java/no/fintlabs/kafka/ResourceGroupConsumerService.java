package no.fintlabs.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.AzureClient;
import no.fintlabs.ConfigGroup;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j

public class ResourceGroupConsumerService {
    private final AzureClient azureClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final ConfigGroup configGroup;
    private final ConcurrentHashMap<String, Optional<ResourceGroup>> resourceGroupCache;
    private Sinks.Many<Tuple2<String, Optional<ResourceGroup>>> resourceGroupSink;
    private final ConcurrentHashMap<String, AzureGroup> azureGroupCache;

    public ResourceGroupConsumerService(
            AzureClient azureClient,
            EntityConsumerFactoryService entityConsumerFactoryService,
            ConfigGroup configGroup,
            ConcurrentHashMap<String, Optional<ResourceGroup>> resourceGroupCache, ConcurrentHashMap<String, AzureGroup> azureGroupCache) {
        this.azureClient = azureClient;
        this.entityConsumerFactoryService = entityConsumerFactoryService;
        this.configGroup = configGroup;
        this.resourceGroupCache = resourceGroupCache;
        this.azureGroupCache = azureGroupCache;

        this.resourceGroupSink = Sinks.many().multicast().onBackpressureBuffer();

        this.resourceGroupSink.asFlux()
                .flatMap(t ->
                                Mono.fromRunnable(() -> updateAzure(t.getT1(), t.getT2()))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .onErrorResume(e -> {
                                            log.error("Graph update failed key={}", t.getT1(), e);
                                            return Mono.empty();
                                        }),
                        20
                )
                .doOnSubscribe(s -> log.info("ResourceGroup pipeline subscribed"))
                .doFinally(sig -> log.error("ResourceGroup pipeline terminated signal={}", sig))
                .subscribe();
    }

    protected void setResourceGroupSink(Sinks.Many<Tuple2<String, Optional<ResourceGroup>>> resourceGroupSink) {
        this.resourceGroupSink = resourceGroupSink;
    }

    @PostConstruct
    public void init() {

        // Initialize azoureGroupCache from Microsoft Graph

        //TODO: Fix sensible throw when parsing wrong data. Non-json-formatted data fails [FKS-214]
        entityConsumerFactoryService.createFactory(
                ResourceGroup.class,
                consumerRecord -> processEntity(
                        consumerRecord.value(), consumerRecord.key()
                )
        ).createContainer(
                EntityTopicNameParameters
                        .builder()
                        .resource("resource-group")
                        .build()
        );
    }

    void updateAzure(String kafkaKey, Optional<ResourceGroup> resourceGroupOptional) {
        String randomUUID = UUID.randomUUID().toString();
        log.debug("Starting updateAzure function {}.", randomUUID);
        ResourceGroup resourceGroup;
        if (resourceGroupOptional.isPresent()) {
            resourceGroup = resourceGroupOptional.get();
            if (resourceGroup.getResourceName() != null && !azureClient.doesGroupExist(resourceGroup.getId())) {
                log.debug("Adding Group to Azure: {}", resourceGroup.getResourceName());
                azureClient.addGroupToAzure(resourceGroup);
            } else {
                if (configGroup.getAllowgroupupdate() && resourceGroup.getIdentityProviderGroupObjectId() != null) {
                    azureClient.updateGroup(resourceGroup);
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
                azureClient.deleteGroup(kafkaKey);
            } else {
                log.warn("ResourceGroupId {} is NOT deleted, as environment parameter allowgroupdelete is set to false", kafkaKey);
            }
        }
        log.debug("Stopping updateAzure function {}.", randomUUID);
    }

    public void processEntity(ResourceGroup resourceGroup, String kafkaKey) {
        synchronized (resourceGroupCache) {
            if (resourceGroupCache.containsKey(kafkaKey)) {
                Optional<ResourceGroup> fromCache = resourceGroupCache.get(kafkaKey);

                if (fromCache.isEmpty() && resourceGroup == null) {
                    log.debug("Duplicate delete for key={}, will STILL process/emit", kafkaKey);
                }

                if (resourceGroup != null && fromCache.isPresent() && resourceGroup.equals(fromCache.get())) {
                    log.debug("Unchanged group for key={} ({}), will STILL process/emit",
                            kafkaKey, resourceGroup.getResourceName());
                }
            }

            Optional<ResourceGroup> next = Optional.ofNullable(resourceGroup);
            if (!resourceGroupCache.containsKey(kafkaKey)) {
                resourceGroupCache.put(kafkaKey, next);
            } else {
                Optional<ResourceGroup> prev = resourceGroupCache.get(kafkaKey);
                if (!next.equals(prev)) {
                    resourceGroupCache.put(kafkaKey, next);
                }
            }

            var r = resourceGroupSink.tryEmitNext(Tuples.of(kafkaKey, next));
            if (r.isFailure()) {
                log.error("Emit failed key={} result={}", kafkaKey, r);
            }
        }
    }



}
