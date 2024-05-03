package no.fintlabs.kafka;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.EntraClient;
import no.fintlabs.Config;
import no.fintlabs.azure.EntraGroupMembership;
import no.fintlabs.azure.EntraGroup;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class ResourceGroupMembershipDownloader {
    @Autowired
    private final EntraClient entraClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final Config config;

    private RestTemplate restTemplate;
    public final FintCache<String, Optional> resourceGroupMembershipsFullCache;

    public final FintCache<String, Optional> entraGroupMembershipsFullCache;
    public final Sinks.Many<Tuple2<String, Optional<ResourceGroupMembership>>> resourceGroupMembershipFullSink;


    public ResourceGroupMembershipDownloader(
            EntraClient entraClient,
            EntityConsumerFactoryService entityConsumerFactoryService,
            Config config,
            FintCache<String, Optional> resourceGroupMembershipFullCache, FintCache<String, Optional> entraGroupMembershipsFullCache) {
        this.entraGroupMembershipsFullCache = entraGroupMembershipsFullCache;
        this.restTemplate = new RestTemplate();
        this.entraClient = entraClient;
        this.entityConsumerFactoryService = entityConsumerFactoryService;
        this.config = config;
        this.resourceGroupMembershipsFullCache = resourceGroupMembershipFullCache;
        this.resourceGroupMembershipFullSink = Sinks.many().unicast().onBackpressureBuffer();
//        this.resourceGroupMembershipSink.asFlux().subscribe(
//                keyAndResourceGroupMembership -> updateAzureWithMembership(keyAndResourceGroupMembership.getT1(), keyAndResourceGroupMembership.getT2())
//        );
    }

    public HttpStatusCode callApi() {
        String apiUrl = "http://localhost:3030/api/assignments/republish";
        HttpEntity<String> requestEntity = new HttpEntity<>(null, null);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(apiUrl, requestEntity, String.class);
        HttpStatusCode StatusCode = responseEntity.getStatusCode();
        return StatusCode;
    }


    public void GetFullMembershipFromKafka() {
        //TODO: Fix sensible throw when parsing wrong data. Non-json-formatted data fails [FKS-214]
        entityConsumerFactoryService.createFactory(ResourceGroupMembership.class, consumerRecord -> processEntityFromKafka(consumerRecord.value(), consumerRecord.key().split("_")[0])
        ).createContainer(
                EntityTopicNameParameters
                        .builder()
                        .resource("resource-group-membership-full")
                        .build()
        );
    }
//
//    private synchronized void updateAzureWithMembership(String kafkakKey, Optional<ResourceGroupMembership> resourceGroupMembership) {
//        String randomUUID = UUID.randomUUID().toString();
//        log.debug("Starting updateAzureWithMembership function {}.", randomUUID);
//
//        if (resourceGroupMembership.isEmpty()) {
//            azureClient.deleteGroupMembership(null, kafkakKey);
//        }
//        else
//        {
//            azureClient.addGroupMembership(resourceGroupMembership.get(), kafkakKey);
//        }
//        log.debug("Stopping updateAzureWithMembership function {}.", randomUUID);
//    }
//
    public void processEntityFromKafka(ResourceGroupMembership resourceGroupMembership, String kafkaKey) {

        if (kafkaKey == null || (resourceGroupMembership != null && (resourceGroupMembership.getAzureGroupRef() == null || resourceGroupMembership.getAzureUserRef() == null))) {
            log.error("Error when processing entity. Kafka key or values is null. Unsupported!. ResourceGroupMembership object: {}",
                    (resourceGroupMembership != null ? resourceGroupMembership : "null"));
            return;
        }

        // Check resourceGroupCache if object is known from before
        log.debug("Processing entity with key: {}", kafkaKey);

        if (resourceGroupMembershipsFullCache.containsKey(kafkaKey)) {
            log.debug("Found key in cache: {}", kafkaKey);

            Optional<ResourceGroupMembership> fromCache = resourceGroupMembershipsFullCache.get(kafkaKey);

            log.debug("From cache: {}", fromCache);

            if (fromCache.isEmpty() && resourceGroupMembership == null) {
                // resourceGroupMembership is a delete message already in cache
                log.debug("Skipping processing of already cached delete group membership message: {}",kafkaKey);
                return;
            }

            if (resourceGroupMembership != null && fromCache.isPresent() && resourceGroupMembership.equals(fromCache.get())){
                // New kafka message, but unchanged resourceGroupMembership from last time
                log.debug("Skipping processing of group membership, as it is unchanged from before: userID: {} groupID {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef() );
                return;
            }
        }
        resourceGroupMembershipsFullCache.put(kafkaKey, Optional.ofNullable(resourceGroupMembership));
        resourceGroupMembershipFullSink.tryEmitNext(Tuples.of(kafkaKey, Optional.ofNullable(resourceGroupMembership)));
    }

    public void processGroupsFromEntraID(EntraGroup entraGroup) {

//        if (azureGroup.getId() == null || (azureGroup != null && azureGroup.getMembers().isEmpty())) {
//            log.error("Error when processing entity. Kafka key or values is null. Unsupported!. ResourceGroupMembership object: {}",
//                    (resourceGroupMembership != null ? resourceGroupMembership : "null"));
//            return;
//        }


        // Check entraGroupCache if object is known from before
        log.debug("Processing EntraGroupName {}, with groupId: {}", entraGroup.getDisplayName(), entraGroup.getId());

        if (entraGroupMembershipsFullCache.containsKey(entraGroup.getId())) {
            log.debug("Found groupId in cache: {}", entraGroup.getId());

            Optional<EntraGroupMembership> entraGroupCache = entraGroupMembershipsFullCache.get(entraGroup.getId());

            log.debug("From cache: {}", entraGroupCache);

//            if (fromCache.isEmpty() && azureGroup == null) {
//                // resourceGroupMembership is a delete message already in cache
//                log.debug("Skipping processing of already cached delete group membership message: {}",kafkaKey);
//                return;
//            }

            if (entraGroupCache.isPresent() && entraGroup.equals(entraGroupCache.get())){
                // Group is unchanged from last time and does not deed to be added to groupCache
                log.debug("Skipping processing of group, as it is unchanged from before: groupID {}", entraGroup.getId());
                return;
            }
        }
        entraGroupMembershipsFullCache.put(entraGroup.getId(), Optional.ofNullable(entraGroup.getMembers()));


    }
}


