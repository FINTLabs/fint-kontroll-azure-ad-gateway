package no.fintlabs.azure;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.producing.ParameterizedTemplateFactory;
import no.fintlabs.kafka.topic.name.EntityTopicNameParameters;
import no.fintlabs.kafka.topic.name.TopicNamePrefixParameters;
import no.fintlabs.kafka.model.ParameterizedProducerRecord;
import no.fintlabs.kafka.producing.ParameterizedTemplate;
import no.fintlabs.kafka.topic.EntityTopicService;
import no.fintlabs.kafka.topic.configuration.CleanupFrequency;
import no.fintlabs.kafka.topic.configuration.EntityTopicConfiguration;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j

public class AzureUserProducerService {

    private final ParameterizedTemplate<AzureUser> azureUserTemplate;
    private final EntityTopicNameParameters entityTopicNameParameters;

    public AzureUserProducerService(
            ParameterizedTemplateFactory parameterizedTemplateFactory,
            EntityTopicService entityTopicService) {

        azureUserTemplate = parameterizedTemplateFactory.createTemplate(AzureUser.class);
        TopicNamePrefixParameters topicNamePrefixParameters = TopicNamePrefixParameters.builder()
                .orgIdApplicationDefault()
                .domainContextApplicationDefault()
                .build();

        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .topicNamePrefixParameters(topicNamePrefixParameters)
                .resourceName("azureuser")
                .build();

        entityTopicService.createOrModifyTopic(entityTopicNameParameters,
                EntityTopicConfiguration.builder()
                        .lastValueRetainedForever()
                        .nullValueRetentionTime(Duration.ofDays(7))
                        .cleanupFrequency(CleanupFrequency.NORMAL)
                        .build());

    }

    public void publish(AzureUser azureUser) {
        azureUserTemplate.send(
                ParameterizedProducerRecord.<AzureUser>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(azureUser.getIdpUserObjectId())
                        .value(azureUser)
                        .build()
        );
    }
}