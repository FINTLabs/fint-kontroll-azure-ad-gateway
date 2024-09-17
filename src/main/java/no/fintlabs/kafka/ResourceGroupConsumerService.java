package no.fintlabs.kafka;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.AzureClient;
import no.fintlabs.ConfigGroup;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j

public class ResourceGroupConsumerService {
    private final AzureClient azureClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final ConfigGroup configGroup;
    private final FintCache<String, Optional> resourceGroupCache;
    private Sinks.Many<Tuple2<String, Optional<ResourceGroup>>> resourceGroupSink;

    public ResourceGroupConsumerService(
            AzureClient azureClient,
            EntityConsumerFactoryService entityConsumerFactoryService,
            ConfigGroup configGroup,
            FintCache<String, Optional> resourceGroupCache) {
        this.azureClient = azureClient;
        this.entityConsumerFactoryService = entityConsumerFactoryService;
        this.configGroup = configGroup;
        this.resourceGroupCache = resourceGroupCache;

        resourceGroupSink = Sinks.many().unicast().onBackpressureBuffer();
        resourceGroupSink.asFlux()
                .parallel(20) // Parallelism with up to 20 threads
                .runOn(Schedulers.boundedElastic())
                .subscribe
                        (keyAndResourceGroup -> {
                            try {
                                updateAzure(keyAndResourceGroup.getT1(), keyAndResourceGroup.getT2());
                            } catch (Exception e) {
                                log.error("Failed to update azure", e);
                            }
                        }
                );
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

    void updateAzure(String kafkaKey, Optional<ResourceGroup> resourceGroupOptional) throws Exception {
        String randomUUID = UUID.randomUUID().toString();
        log.debug("Starting updateAzure function {}.", randomUUID);
        ResourceGroup resourceGroup;
        if (resourceGroupOptional.isPresent()) {
            resourceGroup = resourceGroupOptional.get();
            boolean groupexists;
            try {
                groupexists = azureClient.doesGroupExist(resourceGroup.getId());
            } catch (Exception e) {
                throw e;
            }
            if (resourceGroup.getResourceName() != null && !groupexists) {
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
            // Check resourceGroupCache if object is known from before
            if (resourceGroupCache.containsKey(kafkaKey)) {
                Optional<ResourceGroup> fromCache = resourceGroupCache.get(kafkaKey);
                // Detect if cache contains deletion of resourceGroup from before
                if (fromCache.isEmpty() && resourceGroup == null) {
                    log.debug("Skip processing of entity as cache already contains deleted group on resourceGroupId: {}", kafkaKey);
                    return;
                }
                // Detect if last entry in cache is identical to new entity
                if (resourceGroup != null && fromCache.isPresent() && resourceGroup.equals(fromCache.get())){
                    // New kafka message, but unchanged resourceGroup from last time
                    log.debug("Skip entity as it is unchanged: {}", resourceGroup.getResourceName());
                    return;
                }
            }
            resourceGroupCache.put(kafkaKey, Optional.ofNullable(resourceGroup));
            resourceGroupSink.tryEmitNext(Tuples.of(kafkaKey, Optional.ofNullable(resourceGroup)));
        }
    }
}
