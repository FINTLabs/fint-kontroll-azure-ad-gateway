package no.fintlabs.kafka;


import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.AzureClient;
import no.fintlabs.Config;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.UUID;

@Service
@Slf4j

public class ResourceGroupMembershipConsumerService {
    @Autowired
    private final AzureClient azureClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final Config config;
    private final FintCache<String, ResourceGroupMembership> resourceGroupMembershipCache;
    private final Sinks.Many<Tuple2<String, ResourceGroupMembership>> resourceGroupMembershipSink;

    public ResourceGroupMembershipConsumerService(
            AzureClient azureClient,
            EntityConsumerFactoryService entityConsumerFactoryService,
            Config config,
            FintCache<String, ResourceGroupMembership> resourceGroupMembershipCache) {
        this.azureClient = azureClient;
        this.entityConsumerFactoryService = entityConsumerFactoryService;
        this.config = config;
        this.resourceGroupMembershipCache = resourceGroupMembershipCache;
        this.resourceGroupMembershipSink = Sinks.many().unicast().onBackpressureBuffer();
        this.resourceGroupMembershipSink.asFlux().subscribe(
                keyAndResourceGroupMembership -> updateAzureWithMembership(keyAndResourceGroupMembership.getT1(), keyAndResourceGroupMembership.getT2())
        );
    }

    @PostConstruct
    public void init() {
        //TODO: Fix sensible throw when parsing wrong data. Non-json-formatted data fails [FKS-214]
        entityConsumerFactoryService.createFactory(ResourceGroupMembership.class, consumerRecord -> processEntity(consumerRecord.value(), consumerRecord.key())
        ).createContainer(
                EntityTopicNameParameters
                        .builder()
                        .resource("resource-group-membership")
                        .build()
        );
    }

    private synchronized void updateAzureWithMembership(String kafkakKey, ResourceGroupMembership resourceGroupMembership) {
        String randomUUID = UUID.randomUUID().toString();
        log.debug("Starting updateAzureWithMembership function {}.", randomUUID);

        if (resourceGroupMembership == null) {
            azureClient.deleteGroupMembership(resourceGroupMembership, kafkakKey);
        }
        else
        {
            azureClient.addGroupMembership(resourceGroupMembership, kafkakKey);
        }
        log.debug("Stopping updateAzureWithMembership function {}.", randomUUID);
    }

    public void processEntity(ResourceGroupMembership resourceGroupMembership, String kafkaKey) {

        if (kafkaKey == null) {
            log.error("Error when processing entity. Kafka key (objekt id in kafka) is not set. Unsupported!");
            return;
        }
        synchronized (resourceGroupMembershipCache) {
            // Check resourceGroupCache if object is known from before
            if (resourceGroupMembershipCache.containsKey(kafkaKey)) {
                ResourceGroupMembership fromCache = resourceGroupMembershipCache.get(kafkaKey);
                if (resourceGroupMembershipCache.equals(fromCache)){
                    // New kafka message, but unchanged resourceGroupMembership from last time
                    log.debug("Skipping processing of group membership, as it is unchanged from before: userID: {} groupID {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef() );
                    return;
                }
            }
            resourceGroupMembershipCache.put(kafkaKey, resourceGroupMembership);
            resourceGroupMembershipSink.tryEmitNext(Tuples.of(kafkaKey, resourceGroupMembership));
        }

    }
}


