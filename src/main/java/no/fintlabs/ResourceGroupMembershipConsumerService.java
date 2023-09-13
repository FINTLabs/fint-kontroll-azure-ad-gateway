package no.fintlabs;


import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.requests.GraphServiceClient;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@AllArgsConstructor
@Slf4j

public class ResourceGroupMembershipConsumerService {
    // TODO: Check if this is the same object as in AzureClient

    @Autowired
    private final GraphServiceClient<Request> graphServiceClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;

    @PostConstruct
    public void init() {
        //TODO: Fix sensible throw when parsing wrong data. Non-json-formatted data fails
        //TODO: Fetch topicname from config
        entityConsumerFactoryService.createFactory(ResourceGroupMembership.class, consumerRecord -> processEntity(consumerRecord.value())
        ).createContainer(
                EntityTopicNameParameters
                        .builder()
                        .resource("resource-group-membership")
                        .build()
        );
    }
    public void processEntity(ResourceGroupMembership resourceGroupMembership) {


        DirectoryObject directoryObject = new DirectoryObject();
        directoryObject.id = resourceGroupMembership.userRef;

        try
        {
        graphServiceClient.groups(resourceGroupMembership.resourceRef).members().references()
                .buildRequest()
                .post(directoryObject);
        } catch (GraphServiceException e) {
            // Handle the HTTP response exception here
            if (e.getResponseCode() == 400) {
                // Handle the 400 Bad Request error
                log.info("User already exists in group {}: ", resourceGroupMembership.resourceRef);
            } else {
                // Handle other HTTP errors
                log.error("HTTP Error while updating group {}: " + e.getResponseCode() + " \r" + e.getResponseMessage(),resourceGroupMembership.resourceRef);
            }
        }
    }
}


