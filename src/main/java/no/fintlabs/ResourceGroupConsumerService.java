package no.fintlabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.AssignedLabel;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.DirectoryObjectCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.LinkedList;
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

    private final KafkaTopics kafkaTopics;
    @PostConstruct
    public void init() {
        //TODO: Fix sensible throw when parsing wrong data. Non-json-formatted data fails
        entityConsumerFactoryService.createFactory(ResourceGroup.class, consumerRecord -> processEntity(consumerRecord.value(), consumerRecord.key())
        ).createContainer(
                EntityTopicNameParameters
                        .builder()
                        .resource(kafkaTopics.getResourcegrouptopic())
                        .build()
        );
    }

    public boolean doesGroupExist(String groupName) {
        List<Group> groups = graphServiceClient.groups()
                .buildRequest()
                .get()
                .getCurrentPage();

        for (Group group : groups) {
            if (group.displayName != null && group.displayName.equalsIgnoreCase(groupName)) {
                //log.debug("Group found");
                return true; // Group with the specified name exists
            }
        }
        //log.debug("Group not found");
        return false; // Group with the specified name not found
    }

    public void processEntity(ResourceGroup resourceGroup, String kafkaGroupId) {

        if (resourceGroup.resourceName != null && !doesGroupExist(resourceGroup.resourceName)) {
            log.info("Adding Group to Azure: {}", resourceGroup.resourceName);
            Group group = new Group();
            group.displayName = resourceGroup.resourceName;
            group.mailEnabled = false;
            group.mailNickname = resourceGroup.resourceName.replaceAll("[^a-zA-Z0-9]", ""); // Remove special characters
            group.securityEnabled = true;
            //group.("FINT-Kontroll",group.id)
            Group createdGroup = graphServiceClient.groups()
                    .buildRequest()
                    .post(group);
            DirectoryObject ownerDirectoryObject = new DirectoryObject();
            ownerDirectoryObject.id = config.getEntobjectid();
            graphServiceClient.groups(createdGroup.id).owners().references()
                    .buildRequest()
                    .post(ownerDirectoryObject);
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

