package no.fintlabs.azure;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.producing.ParameterizedTemplateFactory;
import no.fintlabs.kafka.topic.name.EntityTopicNameParameters;
import no.fintlabs.kafka.topic.name.TopicNamePrefixParameters;
import no.fintlabs.kafka.model.ParameterizedProducerRecord;
import no.fintlabs.kafka.producing.ParameterizedTemplate;
import no.fintlabs.kafka.topic.EntityTopicService;
import no.fintlabs.kafka.topic.configuration.CleanupFrequency;
import no.fintlabs.kafka.topic.configuration.EntityTopicConfiguration;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class AzureGroupProducerService {

    private final ParameterizedTemplate<AzureGroup> azureGroupTemplate;
    private final EntityTopicNameParameters entityTopicNameParameters;
    private Sinks.Many<AzureGroup> azureGroupSink;

    public AzureGroupProducerService (
            ParameterizedTemplateFactory parameterizedTemplateFactory,
            EntityTopicService entityTopicService) {

        this.azureGroupSink = Sinks.many().unicast().onBackpressureBuffer();
        this.azureGroupSink.asFlux()
                .parallel(20) // Parallelism with up to 20 threads
                .runOn(Schedulers.boundedElastic())
                .subscribe(this::publishNewGroup);

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
    }

    public void processGroup(AzureGroup azureGroup) {
        azureGroupSink.tryEmitNext(azureGroup);
    }

    public void publishNewGroup(AzureGroup azureGroup) {
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
