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

public class AzureUserProducerService {
    private final EntityProducer<AzureUser> entityProducer;
    private final EntityTopicNameParameters entityTopicNameParameters;

    public AzureUserProducerService(
            EntityTopicService entityTopicService,
            EntityProducerFactory entityProducerFactory) {

        entityProducer = entityProducerFactory.createProducer(AzureUser.class);
        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource(AzureUser.class.getName())
                .build();
        entityTopicService.ensureTopic(entityTopicNameParameters,0);
    }
    public void publish(AzureUser azureUser) {
        entityProducer.send(
                EntityProducerRecord.<AzureUser>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(azureUser.getId())
                        .value(azureUser)
                        .build()
        );
    }
}