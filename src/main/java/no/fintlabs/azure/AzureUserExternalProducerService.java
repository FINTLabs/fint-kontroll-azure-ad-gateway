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

public class AzureUserExternalProducerService {
    private final EntityProducer<AzureUserExternal> entityProducer;
    private final EntityTopicNameParameters entityTopicNameParameters;

    public AzureUserExternalProducerService(
            EntityTopicService entityTopicService,
            EntityProducerFactory entityProducerFactory) {

        entityProducer = entityProducerFactory.createProducer(AzureUserExternal.class);
        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource(AzureUserExternal.class.getSimpleName())
                .build();
        entityTopicService.ensureTopic(entityTopicNameParameters,0);
    }
    public void publish(AzureUserExternal azureUserExternal) {
        entityProducer.send(
                EntityProducerRecord.<AzureUserExternal>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(azureUserExternal.getIdpUserObjectId())
                        .value(azureUserExternal)
                        .build()
        );
    }
}