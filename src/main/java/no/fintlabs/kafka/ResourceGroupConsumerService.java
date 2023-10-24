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
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import javax.management.openmbean.OpenType;
import java.util.*;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ResourceGroupConsumerService {
    // TODO: Check if this is the same object as in AzureClient

    @Autowired
    private final AzureClient azureClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final ConfigGroup configGroup;


    @PostConstruct
    public void init() {
        //TODO: Fix sensible throw when parsing wrong data. Non-json-formatted data fails
        entityConsumerFactoryService.createFactory(ResourceGroup.class, consumerRecord -> processEntity(consumerRecord.value(), consumerRecord.key())
        ).createContainer(
                EntityTopicNameParameters
                        .builder()
                        .resource("resource-group")
                        .build()
        );
    }

    public void processEntity(ResourceGroup resourceGroup, String kafkaGroupId) {

        // TODO: Split doesGroupExist to POST or PUT
        if (resourceGroup.resourceName != null && !azureClient.doesGroupExist(resourceGroup.id)) {
            azureClient.addGroupToAzure(resourceGroup);
        }
        else if(resourceGroup.resourceName == null)
        {
            azureClient.deleteGroup(kafkaGroupId);
        }
        else
        {
            log.debug("Group not created as it already exists: {}", resourceGroup.resourceName);
            azureClient.updateGroup(resourceGroup);
        }
    }
 }

