package no.fintlabs.kafka;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.AzureClient;
import no.fintlabs.Config;
import no.fintlabs.azure.AzureGroupMembership;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ResourceGroupMembershipConsumerService {
    private final AzureClient azureClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final ConcurrentHashMap<String, Optional<ResourceGroupMembership>> resourceGroupMembershipCache;
    private Sinks.Many<Tuple2<String, Optional<ResourceGroupMembership>>> resourceGroupMembershipSink;
    private final ConcurrentHashMap<String, AzureGroupMembership> azureGroupMembershipCache;

    public ResourceGroupMembershipConsumerService(
            AzureClient azureClient,
            EntityConsumerFactoryService entityConsumerFactoryService,
            ConcurrentHashMap<String, Optional<ResourceGroupMembership>> resourceGroupMembershipCache,
            ConcurrentHashMap<String, AzureGroupMembership> azureGroupMembershipCache, ConcurrentHashMap<String, AzureGroupMembership> azureGroupMembershipCache1) {
        this.azureClient = azureClient;
        this.entityConsumerFactoryService = entityConsumerFactoryService;
        this.resourceGroupMembershipCache = resourceGroupMembershipCache;
        this.azureGroupMembershipCache = azureGroupMembershipCache;
        this.resourceGroupMembershipSink = Sinks.many().multicast().onBackpressureBuffer();

        this.resourceGroupMembershipSink.asFlux()
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

    protected void setResourceGroupMembershipSink(Sinks.Many<Tuple2<String, Optional<ResourceGroupMembership>>> resourceGroupMembershipSink) {
        this.resourceGroupMembershipSink = resourceGroupMembershipSink;
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

    void updateAzureWithMembership(String kafkaKey, Optional<ResourceGroupMembership> membershipOpt) {
        log.debug("Starting updateAzureWithMembership key={}.", kafkaKey);
        try {
            if (membershipOpt.isEmpty()) {
                azureClient.deleteGroupMembership(kafkaKey);
            } else {
                azureClient.addGroupMembership(membershipOpt.get(), kafkaKey);
            }
            log.debug("updateAzureWithMembership OK key={}", kafkaKey);
        } catch (Exception e) {
            log.error("updateAzureWithMembership FAILED key={}", kafkaKey, e);
        } finally {
            log.debug("Stopping updateAzureWithMembership key={}", kafkaKey);
        }
    }

    public void processEntity(ResourceGroupMembership resourceGroupMembership, String kafkaKey) {
        if (kafkaKey == null || (resourceGroupMembership != null
                && (resourceGroupMembership.getAzureGroupRef() == null || resourceGroupMembership.getAzureUserRef() == null))) {
            log.error("Error when processing entity. Kafka key or values is null. Unsupported!. ResourceGroupMembership object: {}",
                    (resourceGroupMembership != null ? resourceGroupMembership : "null"));
            return;
        }

        synchronized (resourceGroupMembershipCache) {
            log.debug("Processing entity with key: {}", kafkaKey);

            if (resourceGroupMembershipCache.containsKey(kafkaKey)) {
                Optional<ResourceGroupMembership> fromCache = resourceGroupMembershipCache.get(kafkaKey);

                if (fromCache.isEmpty() && resourceGroupMembership == null) {
                    log.debug("Duplicate delete (tombstone) for membership key={}, will STILL process/emit", kafkaKey);
                }

                if (resourceGroupMembership != null && fromCache.isPresent() && resourceGroupMembership.equals(fromCache.get())) {
                    log.debug("Unchanged membership userID={} groupID={} key={} - will STILL process/emit",
                            resourceGroupMembership.getAzureUserRef(),
                            resourceGroupMembership.getAzureGroupRef(),
                            kafkaKey);
                }
            }

            Optional<ResourceGroupMembership> next = Optional.ofNullable(resourceGroupMembership);
            if (!resourceGroupMembershipCache.containsKey(kafkaKey)) {
                resourceGroupMembershipCache.put(kafkaKey, next);
            } else {
                Optional<ResourceGroupMembership> prev = resourceGroupMembershipCache.get(kafkaKey);
                if (!next.equals(prev)) {
                    resourceGroupMembershipCache.put(kafkaKey, next);
                }
            }

            var r = resourceGroupMembershipSink.tryEmitNext(Tuples.of(kafkaKey, next));
            if (r.isFailure()) {
                log.error("Emit failed key={} result={}", kafkaKey, r);
            } else {
                log.debug("Emit OK key={} (delete={})", kafkaKey, next.isEmpty());
            }
        }
    }




}
