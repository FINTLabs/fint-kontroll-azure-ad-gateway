package no.fintlabs.kafka;


import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.Config;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j

public class ResourceGroupMembershipConsumerService {
    // TODO: Check if this is the same object as in AzureClient

    @Autowired
    private final GraphServiceClient<Request> graphServiceClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final Config config;

    @PostConstruct
    public void init() {
        //TODO: Fix sensible throw when parsing wrong data. Non-json-formatted data fails
        entityConsumerFactoryService.createFactory(ResourceGroupMembership.class, consumerRecord -> processEntity(consumerRecord.value(), consumerRecord.key())
        ).createContainer(
                EntityTopicNameParameters
                        .builder()
                        .resource("resource-group-membership")
                        .build()
        );
    }
    public void processEntity(ResourceGroupMembership resourceGroupMembership, String resourceGroupMembershipKey) {


        if (resourceGroupMembership.id != null) {
            DirectoryObject directoryObject = new DirectoryObject();
            directoryObject.id = resourceGroupMembership.azureUserRef;

            try {
                Objects.requireNonNull(graphServiceClient.groups(resourceGroupMembership.azureGroupRef).members().references())
                        .buildRequest()
                        .post(directoryObject);
                log.info("User {} added to group {}: ", resourceGroupMembership.azureUserRef, resourceGroupMembership.azureGroupRef);
            } catch (GraphServiceException e) {
                // Handle the HTTP response exception here
                if (e.getResponseCode() == 400) {
                    // Handle the 400 Bad Request error
                    log.warn("User {} already exists in group {} or azureGroupRef is not correct: ", resourceGroupMembership.azureUserRef, resourceGroupMembership.azureGroupRef);
                } else {
                    // Handle other HTTP errors
                    log.error("HTTP Error while updating group {}: " + e.getResponseCode() + " \r" + e.getResponseMessage(), resourceGroupMembership.azureGroupRef);
                }
            }
        }
        else
        {
            String group = resourceGroupMembershipKey.split("_")[0];
            String user = resourceGroupMembershipKey.split("_")[1];

            try {
                Objects.requireNonNull(graphServiceClient.groups(group)
                        .members(user)
                        .reference()
                        .buildRequest()
                        .delete());
                log.warn("User: {} removed from group: {}", user, group);
            }
            catch (GraphServiceException e)
            {
                log.error("HTTP Error while removing user from group {}: " + e.getResponseCode() + " \r" + e.getResponseMessage());
            }

        }
    }
}


