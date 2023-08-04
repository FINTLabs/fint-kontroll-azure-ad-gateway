package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@AllArgsConstructor
@Slf4j
public class ResourceGroupConsumerService {
    private final EntityConsumerFactoryService entityConsumerFactoryService;

    @PostConstruct
    public void init() {
        //TODO: Fix sensible throw when parsing wrong data
        //TODO: Fetch from config
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
    }
}
