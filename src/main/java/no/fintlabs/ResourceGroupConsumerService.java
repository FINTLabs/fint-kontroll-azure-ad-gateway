package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNamePatternParameters;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@AllArgsConstructor
@Slf4j
public class ResourceGroupConsumerService {
    private final EntityConsumerFactoryService entityConsumerFactoryService;

    @PostConstruct
    public void init() {
        entityConsumerFactoryService.createFactory(ResourceGroup.class, consumerRecord -> processEntity(consumerRecord.value())
        ).createContainer(
                EntityTopicNamePatternParameters
                        .builder()
                        .orgId(FormattedTopicComponentPattern.any())
                        .domainContext(FormattedTopicComponentPattern.any())
                        .resource( FormattedTopicComponentPattern.endingWith("resource-group"))
                        .build()
        );
        //TODO: Fetch from config
    }
    private void processEntity(ResourceGroup resourceGroup) {
        log.info("test123\n");
    }
}
