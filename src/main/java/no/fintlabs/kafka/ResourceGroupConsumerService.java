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
    private final GraphServiceClient<Request> graphServiceClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final Config config;
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

    public boolean doesGroupExist(ResourceGroup resourceGroup) {
        List<Group> groups = graphServiceClient.groups()
                .buildRequest()
                .select(String.format("id,displayName,description,%s", configGroup.getFintkontrollidattribute()))
                .get()
                .getCurrentPage();

        for (Group group : groups) {
            if (group.additionalDataManager().get(configGroup.getFintkontrollidattribute()) != null)
            {
                if (group.additionalDataManager().get(configGroup.getFintkontrollidattribute()).getAsString().equals(resourceGroup.id))
                {
                    return true; // Group with the specified ResourceID found
                }
            }
        }
        // Group with resourceID not found
        return false;
    }

    public void processEntity(ResourceGroup resourceGroup, String kafkaGroupId) {

        if (resourceGroup.resourceName != null && !doesGroupExist(resourceGroup)) {
            log.info("Adding Group to Azure: {}", resourceGroup.resourceName);
            Group group = new Group();
            group.displayName = resourceGroup.resourceName;
            group.mailEnabled = false;
            group.mailNickname = resourceGroup.resourceName.replaceAll("[^a-zA-Z0-9]", ""); // Remove special characters
            group.securityEnabled = true;
            group.additionalDataManager().put(configGroup.getFintkontrollidattribute(), new JsonPrimitive(resourceGroup.id));

            String owner = "https://graph.microsoft.com/v1.0/directoryObjects/" + config.getEntobjectid();
            var owners = new JsonArray();
            owners.add(owner);
            group.additionalDataManager().put("owners@odata.bind",  owners);

            graphServiceClient.groups()
                    .buildRequest()
                    .post(group);

        }
        else if(resourceGroup.resourceName == null)
        {
            graphServiceClient.groups(kafkaGroupId)
                    .buildRequest()
                            .delete();

            log.info("Group with kafkaId {} deleted ", kafkaGroupId);
        }
        else
        {
            log.info("Group not created as it already exists: {}", resourceGroup.resourceName);
        }
    }
 }

