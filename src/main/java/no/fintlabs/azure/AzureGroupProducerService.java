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
public class AzureGroupProducerService {

    private final ParameterizedTemplate<AzureGroup> azureGroupTemplate;
    private final EntityTopicNameParameters entityTopicNameParameters;

    public AzureGroupProducerService (
            ParameterizedTemplateFactory parameterizedTemplateFactory,
            EntityTopicService entityTopicService) {

        azureGroupTemplate = parameterizedTemplateFactory.createTemplate(AzureGroup.class);

        TopicNamePrefixParameters topicNamePrefixParameters = TopicNamePrefixParameters.builder()
                .orgIdApplicationDefault()
                .domainContextApplicationDefault()
                .build();

        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .topicNamePrefixParameters(topicNamePrefixParameters)
                .resourceName("azuread-resource-group")
                .build();

        entityTopicService.createOrModifyTopic(entityTopicNameParameters,
                EntityTopicConfiguration.builder()
                        .lastValueRetainedForever()
                        .nullValueRetentionTime(Duration.ofDays(7))
                        .cleanupFrequency(CleanupFrequency.NORMAL)
                        .build());

//        entityProducer = entityProducerFactory.createProducer(AzureGroup.class);
//        entityTopicNameParameters = EntityTopicNameParameters
//                .builder()
//                .resource("azuread-resource-group")
//                .build();
//        entityTopicService.ensureTopic(entityTopicNameParameters,0);
    }

    public void publish(AzureGroup azureGroup) {
        if (azureGroup.resourceGroupID != null) {
            azureGroupTemplate.send(
                    ParameterizedProducerRecord.<AzureGroup>builder()
                            .topicNameParameters(entityTopicNameParameters)
                            .key(azureGroup.getId())
                            .value(azureGroup)
                            .build()
            );
        }
    }
}
