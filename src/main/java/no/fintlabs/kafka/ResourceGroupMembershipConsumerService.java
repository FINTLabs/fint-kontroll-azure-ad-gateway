package no.fintlabs.kafka;


import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.AzureClient;
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
    @Autowired
    private final AzureClient azureClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final Config config;

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
    public void processEntity(ResourceGroupMembership resourceGroupMembership, String resourceGroupMembershipKey) {

        if (resourceGroupMembershipKey == null) {
            log.error("Error when processing entity. Kafka key (objekt id in kafka) is not set. Unsupported!");
            return;
        }
        // Already existing membership
        if (resourceGroupMembership == null) {
            azureClient.deleteGroupMembership(resourceGroupMembership, resourceGroupMembershipKey);
        }
        else
        {
            azureClient.addGroupMembership(resourceGroupMembership, resourceGroupMembershipKey);
        }
    }
}


