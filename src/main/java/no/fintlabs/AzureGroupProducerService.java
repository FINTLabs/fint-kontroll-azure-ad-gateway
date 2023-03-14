package no.fintlabs;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
//@AllArgsConstructor
public class AzureGroupProducerService {

    private final EntityProducer<AzureGroup> entityProducer;
    private final EntityTopicNameParameters entityTopicNameParameters;

    public AzureGroupProducerService (
            EntityTopicService entityTopicService,
            EntityProducerFactory entityProducerFactory) {

        entityProducer = entityProducerFactory.createProducer(AzureGroup.class);
        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("azuregroup")
                .build();
        entityTopicService.ensureTopic(entityTopicNameParameters,0);
    }
    public void publish(AzureGroup azureGroup) {
        entityProducer.send(
                EntityProducerRecord.<AzureGroup>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(azureGroup.getId())
                        .value(azureGroup)
                        .build()
        );
    }
}