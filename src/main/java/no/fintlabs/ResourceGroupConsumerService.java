package no.fintlabs;

import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import okhttp3.Request;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.LinkedList;

@Service
@AllArgsConstructor
@Slf4j
public class ResourceGroupConsumerService {
    // TODO: Check if this is the same object as in AzureClient
    private final GraphServiceClient<Request> graphServiceClient;
    private final EntityConsumerFactoryService entityConsumerFactoryService;

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
    private void processEntity(ResourceGroup resourceGroup) {
        //TODO: Create group in Azure
        log.info("test123\n");

        Group group = new Group();
        group.description = "fint-kontroll-testgroup";
        group.displayName = "FINT kontroll group";
        //LinkedList<String> groupTypesList = new LinkedList<String>();
        //groupTypesList.add("Security");
        //group.groupTypes = groupTypesList;
        //group.groupTypes = new LinkedList<String>(Arrays.asList("Security"));
        group.mailEnabled = false;
        //group.mailNickname = "library";
        group.securityEnabled = true;

        graphServiceClient.groups()
                          .buildRequest()
                          .post(group);

    }
}
