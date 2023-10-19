package no.fintlabs.azure;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AzureGroupProducerService {

    private final EntityProducer<AzureGroup> entityProducer;
    private final EntityTopicNameParameters entityTopicNameParameters;


    public AzureGroupProducerService (
            EntityTopicService entityTopicService,
            EntityProducerFactory entityProducerFactory) {


        entityProducer = entityProducerFactory.createProducer(AzureGroup.class);
        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("azuread-resource-group")
                .build();
        entityTopicService.ensureTopic(entityTopicNameParameters,0);
    }
    public void publish(AzureGroup azureGroup) {
        if (azureGroup.fintKontrollRoleId != null) {
            entityProducer.send(
                    EntityProducerRecord.<AzureGroup>builder()
                            .topicNameParameters(entityTopicNameParameters)
                            .key(azureGroup.getId())
                            .value(azureGroup)
                            .build()
            );
        }
    }
}
