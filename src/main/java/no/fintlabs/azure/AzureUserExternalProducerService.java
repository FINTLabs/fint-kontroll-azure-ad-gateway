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

public class AzureUserExternalProducerService {
    private final ParameterizedTemplate<AzureUserExternal> azureUserExternalTemplate;
    private final EntityTopicNameParameters entityTopicNameParameters;

    public AzureUserExternalProducerService(
            ParameterizedTemplateFactory parameterizedTemplateFactory,
            EntityTopicService entityTopicService) {
        azureUserExternalTemplate = parameterizedTemplateFactory.createTemplate(AzureUserExternal.class);

        TopicNamePrefixParameters topicNamePrefixParameters = TopicNamePrefixParameters.builder()
                .orgIdApplicationDefault()
                .domainContextApplicationDefault()
                .build();

        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .topicNamePrefixParameters(topicNamePrefixParameters)
                .resourceName("AzureUserExternal")
                .build();

        entityTopicService.createOrModifyTopic(entityTopicNameParameters,
                EntityTopicConfiguration.builder()
                        .lastValueRetainedForever()
                        .nullValueRetentionTime(Duration.ofDays(7))
                        .cleanupFrequency(CleanupFrequency.NORMAL)
                        .build());

    }
    public void publish(AzureUserExternal azureUserExternal) {
        azureUserExternalTemplate.send(
                ParameterizedProducerRecord.<AzureUserExternal>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(azureUserExternal.getIdpUserObjectId())
                        .value(azureUserExternal)
                        .build()
        );
    }
}