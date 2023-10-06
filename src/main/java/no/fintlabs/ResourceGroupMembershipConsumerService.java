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

    private final KafkaTopics kafkaTopics;

    @PostConstruct
    public void init() {
        //TODO: Fix sensible throw when parsing wrong data. Non-json-formatted data fails
        entityConsumerFactoryService.createFactory(ResourceGroupMembership.class, consumerRecord -> processEntity(consumerRecord.value(), consumerRecord.key())
        ).createContainer(
                EntityTopicNameParameters
                        .builder()
                        .resource(kafkaTopics.getResourcegroupmembertopic())
                        .build()
        );
    }
    public void processEntity(ResourceGroupMembership resourceGroupMembership, String resourceGroupMembershipKey) {


        if (resourceGroupMembership.id != null) {
            DirectoryObject directoryObject = new DirectoryObject();
            directoryObject.id = resourceGroupMembership.userRef;

            try {
                Objects.requireNonNull(graphServiceClient.groups(resourceGroupMembership.resourceRef).members().references())
                        .buildRequest()
                        .post(directoryObject);
                log.info("User {} added to group {}: ", resourceGroupMembership.userRef, resourceGroupMembership.resourceRef);
            } catch (GraphServiceException e) {
                // Handle the HTTP response exception here
                if (e.getResponseCode() == 400) {
                    // Handle the 400 Bad Request error
                    log.info("User {} already exists in group {}: ", resourceGroupMembership.userRef, resourceGroupMembership.resourceRef);
                } else {
                    // Handle other HTTP errors
                    log.error("HTTP Error while updating group {}: " + e.getResponseCode() + " \r" + e.getResponseMessage(), resourceGroupMembership.resourceRef);
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
                log.info("User: {} removed from group: {}", user, group);
            }
            catch (GraphServiceException e)
            {
                log.error("HTTP Error while removing user from group {}: " + e.getResponseCode() + " \r" + e.getResponseMessage());
            }

        }
    }
}


