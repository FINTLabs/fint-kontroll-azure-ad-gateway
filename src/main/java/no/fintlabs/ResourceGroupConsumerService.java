package no.fintlabs;

import com.microsoft.graph.models.AssignedLabel;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
//TODO: Change PostConstruct to jakarta when SB -> 3.x
//import jakarta.annotation.PostConstruct;
import javax.annotation.PostConstruct;
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

    private final EntityCacheConfiguration entityCacheConfiguration;
    @PostConstruct
    public void init() {
        //TODO: Fix sensible throw when parsing wrong data. Non-json-formatted data fails
        //TODO: Fetch topicname from config
        entityConsumerFactoryService.createFactory(ResourceGroup.class, consumerRecord -> processEntity(consumerRecord.value())
        ).createContainer(
                EntityTopicNameParameters
                        .builder()
                        .resource("resource-group")
                        .build()
        );
    }
    private boolean doesGroupExist(String groupName) {
        List<Group> groups = graphServiceClient.groups()
                .buildRequest()
                .get()
                .getCurrentPage();

        for (Group group : groups) {
            if (group.displayName != null && group.displayName.equalsIgnoreCase(groupName)) {
                log.debug("Group found");
                return true; // Group with the specified name exists

            }
        }
        log.debug("group not found");
        return false; // Group with the specified name not found
    }
    private void processEntity(ResourceGroup resourceGroup) {
        //TODO: Create group in Azure
        log.info("Adding Group {} to Azure\n",resourceGroup.resourceName );


            if(!doesGroupExist(resourceGroup.resourceName)) {
                log.info("Creating group");
                Group group = new Group();
                group.description = resourceGroup.resourceName;
                group.displayName = resourceGroup.resourceName;
                group.mailEnabled = false;
                group.mailNickname = resourceGroup.resourceName.replace(" ", "").trim();
                group.securityEnabled = true;

                graphServiceClient.groups()
                        .buildRequest()
                        .post(group);
            }
        }

    }

