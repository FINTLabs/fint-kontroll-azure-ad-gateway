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

    private final EntityProducer<EntraGroup> entityProducer;
    private final EntityTopicNameParameters entityTopicNameParameters;
    public AzureGroupProducerService (
            EntityTopicService entityTopicService,
            EntityProducerFactory entityProducerFactory) {

        entityProducer = entityProducerFactory.createProducer(EntraGroup.class);
        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("azuread-resource-group")
                .build();
        entityTopicService.ensureTopic(entityTopicNameParameters,0);
    }

    public void publish(EntraGroup entraGroup) {
        if (entraGroup.resourceGroupID != null) {
            entityProducer.send(
                    EntityProducerRecord.<EntraGroup>builder()
                            .topicNameParameters(entityTopicNameParameters)
                            .key(entraGroup.getId())
                            .value(entraGroup)
                            .build()
            );
        }
    }
}
