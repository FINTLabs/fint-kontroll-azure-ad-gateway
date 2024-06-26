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
import reactor.core.scheduler.Schedulers;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j

public class ResourceGroupMembershipConsumerService {
    @Autowired
    private final AzureClient azureClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final Config config;
    private final FintCache<String, Optional> resourceGroupMembershipCache;
    //private final FintCache<UUID, Optional> resourceGroupMembersCache;
    private final Sinks.Many<Tuple2<String, Optional<ResourceGroupMembership>>> resourceGroupMembershipSink;

    public ResourceGroupMembershipConsumerService(
            AzureClient azureClient,
            EntityConsumerFactoryService entityConsumerFactoryService,
            Config config,
            FintCache<String, Optional> resourceGroupMembershipCache,
            FintCache<UUID, Optional> resourceGroupMembersCache) {
        this.azureClient = azureClient;
        this.entityConsumerFactoryService = entityConsumerFactoryService;
        this.config = config;
        this.resourceGroupMembershipCache = resourceGroupMembershipCache;
        //this.resourceGroupMembersCache = resourceGroupMembersCache;
        this.resourceGroupMembershipSink = Sinks.many().unicast().onBackpressureBuffer();
        this.resourceGroupMembershipSink.asFlux()
                //.parallel(20) // Parallelism with up to 20 threads
                //.runOn(Schedulers.parallel())
                .subscribe
                        (keyAndResourceGroupMembership ->
                                updateAzureWithMembership(keyAndResourceGroupMembership.getT1(), keyAndResourceGroupMembership.getT2())
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

    private void updateAzureWithMembership(String kafkakKey, Optional<ResourceGroupMembership> resourceGroupMembership) {
        String randomUUID = UUID.randomUUID().toString();
        log.debug("Starting updateAzureWithMembership function {}.", randomUUID);

        if (resourceGroupMembership.isEmpty()) {
            azureClient.deleteGroupMembership(null, kafkakKey);
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


