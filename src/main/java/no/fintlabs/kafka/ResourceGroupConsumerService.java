package no.fintlabs.kafka;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.EntraClient;
import no.fintlabs.ConfigGroup;
import no.fintlabs.azure.EntraGroup;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;

@Service
@Slf4j

public class ResourceGroupConsumerService {
    private final EntraClient entraClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final ConfigGroup configGroup;
    private final FintCache<String, ResourceGroup> resourceGroupCache;
    private final FintCache<String, EntraGroup> azureGroupCache;
    private final Sinks.Many<Tuple2<String, ResourceGroup>> resourceGroupSink;
    public ResourceGroupConsumerService(
            EntraClient entraClient,
            EntityConsumerFactoryService entityConsumerFactoryService,
            ConfigGroup configGroup,
            FintCache<String, ResourceGroup> resourceGroupCache,
            FintCache<String, EntraGroup> azureGroupCache) {
        this.entraClient = entraClient;
        this.entityConsumerFactoryService = entityConsumerFactoryService;
        this.configGroup = configGroup;
        this.resourceGroupCache = resourceGroupCache;
        this.azureGroupCache = azureGroupCache;

        resourceGroupSink = Sinks.many().unicast().onBackpressureBuffer();
        resourceGroupSink.asFlux().subscribe(
                keyAndResourceGroup -> updateAzure(keyAndResourceGroup.getT1(), keyAndResourceGroup.getT2())
        );
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

    private synchronized void updateAzure(String kafkaKey, ResourceGroup resourceGroup) {
        String randomUUID = UUID.randomUUID().toString();
        log.debug("Starting updateAzure function {}.", randomUUID);
        //azureService.handleChangedResource
        // TODO: Split doesGroupExist to POST or PUT. Relates to [FKS-200] and [FKS-202]
        if (resourceGroup.getResourceName() != null && !entraClient.doesGroupExist(resourceGroup.getId())) {
            log.debug("Adding Group to Azure: {}", resourceGroup.getResourceName());
            entraClient.addGroupToAzure(resourceGroup);
        } else if (resourceGroup.getResourceName() == null) {
            log.debug("Delete group from Azure, {}",resourceGroup.getResourceName());
            entraClient.deleteGroup(kafkaKey);
        } else {
            if (configGroup.getAllowgroupupdate()) {
                entraClient.updateGroup(resourceGroup);
                log.info("Updated group with groupId {}",resourceGroup.getIdentityProviderGroupObjectId());
            }
            else
            {
                log.debug("GroupId {} is NOT updated, as environmentparameter allowgroupupdate is set to false",resourceGroup.getIdentityProviderGroupObjectId() );
            }
        }
        log.debug("Stopping updateAzure function {}.", randomUUID);
    }

    public void processEntity(ResourceGroup resourceGroup, String kafkaKey) {
        synchronized (resourceGroupCache) {
            // Check resourceGroupCache if object is known from before
            if (resourceGroupCache.containsKey(kafkaKey)) {
                ResourceGroup fromCache = resourceGroupCache.get(kafkaKey);
                if (resourceGroup.equals(fromCache)){
                    // New kafka message, but unchanged resourceGroup from last time
                    log.debug("Skip entity as it is unchanged: {}", resourceGroup.getResourceName());
                    return;
                }
            }
            resourceGroupCache.put(kafkaKey, resourceGroup);
            resourceGroupSink.tryEmitNext(Tuples.of(kafkaKey, resourceGroup));
        }
    }
 }
