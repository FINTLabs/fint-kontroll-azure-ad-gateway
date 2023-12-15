package no.fintlabs.kafka;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.DirectoryObjectCollectionPage;
import com.microsoft.graph.requests.ExtensionCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.AzureClient;
import no.fintlabs.Config;
import no.fintlabs.ConfigGroup;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import okhttp3.Request;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.management.openmbean.OpenType;
import java.util.*;
import java.util.List;

@Service
@Slf4j

public class ResourceGroupConsumerService {
    private final AzureClient azureClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final ConfigGroup configGroup;
    private final FintCache<String, ResourceGroup> resourceGroupCache;
    private final FintCache<String, AzureGroup> azureGroupCache;
    private final Sinks.Many<Tuple2<String, ResourceGroup>> resourceGroupSink;
    public ResourceGroupConsumerService(
            AzureClient azureClient,
            EntityConsumerFactoryService entityConsumerFactoryService,
            ConfigGroup configGroup,
            FintCache<String, ResourceGroup> resourceGroupCache,
            FintCache<String, AzureGroup> azureGroupCache) {
        this.azureClient = azureClient;
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
        // TODO: Split doesGroupExist to POST or PUT. Relates to [FKS-200] and [FKS-202]
        if (resourceGroup.getResourceName() != null && !azureClient.doesGroupExist(resourceGroup.getId())) {
            log.debug("Adding Group to Azure: {}", resourceGroup.getResourceName());
            azureClient.addGroupToAzure(resourceGroup);
        } else if (resourceGroup.getResourceName() == null) {
            log.debug("Delete group from Azure, {}",resourceGroup.getResourceName());
            azureClient.deleteGroup(kafkaKey);
        } else {
            log.debug("Group not created as it already exists: {}", resourceGroup.getResourceName());
            azureClient.updateGroup(resourceGroup);
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
                    log.debug("Skip element as it is unchanged: {}", resourceGroup.getResourceName());
                    return;
                }
            }
            resourceGroupCache.put(kafkaKey, resourceGroup);
            resourceGroupSink.tryEmitNext(Tuples.of(kafkaKey, resourceGroup));
        }
    }
 }
