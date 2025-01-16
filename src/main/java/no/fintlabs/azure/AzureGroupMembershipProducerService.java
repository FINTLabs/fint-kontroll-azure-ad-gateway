package no.fintlabs.azure;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.ResourceGroupMembership;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j

public class AzureGroupMembershipProducerService {

    private final ParameterizedTemplate<AzureGroupMembership> azureGroupMembershipTemplate;
    private final EntityTopicNameParameters entityTopicNameParameters;
    private Sinks.Many<Tuple2<String,AzureGroupMembership>> azureGroupMembershipSink;

    public AzureGroupMembershipProducerService(
            ParameterizedTemplateFactory parameterizedTemplateFactory,
            EntityTopicService entityTopicService)
    {
        this.azureGroupMembershipSink = Sinks.many().unicast().onBackpressureBuffer();
        this.azureGroupMembershipSink.asFlux()
                .parallel(20) // Parallelism with up to 20 threads
                .runOn(Schedulers.boundedElastic())
                .subscribe(keyAndAzureMembership ->
                        publishMemberships(keyAndAzureMembership.getT1(), keyAndAzureMembership.getT2()));

        azureGroupMembershipTemplate = parameterizedTemplateFactory.createTemplate(AzureGroupMembership.class);

        TopicNamePrefixParameters topicNamePrefixParameters = TopicNamePrefixParameters.builder()
                .orgIdApplicationDefault()
                .domainContextApplicationDefault()
                .build();

        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .topicNamePrefixParameters(topicNamePrefixParameters)
                .resourceName("azuread-resource-group-membership")
                .build();

        entityTopicService.createOrModifyTopic(entityTopicNameParameters,
                EntityTopicConfiguration.builder()
                        .lastValueRetainedForever()
                        .nullValueRetentionTime(Duration.ofDays(7))
                        .cleanupFrequency(CleanupFrequency.NORMAL)
                        .build());

    }

    public void processMembership(String action, AzureGroupMembership azureMembership) {
        azureGroupMembershipSink.tryEmitNext(Tuples.of(action, azureMembership));
    }

    private void publishMemberships(String action, AzureGroupMembership azureMembership) {
        log.debug("Starting publishMemberships function {}.", azureMembership.getId());
        String kafkaKey = azureMembership.getId();
        if (Objects.equals(action, "removed")) {
            publishDeletedMembership(kafkaKey);
        } else {
            publishAddedMembership(azureMembership);
        }
        log.debug("Stopping publishMemberships function {}.", kafkaKey);
    }

    public void publishDeletedMembership(String membershipKey) {
        azureGroupMembershipTemplate.send(
                ParameterizedProducerRecord.<AzureGroupMembership>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(membershipKey)
                        .value(null)
                        .build()
        );
    }
    public void publishAddedMembership(AzureGroupMembership object) {
        azureGroupMembershipTemplate.send(
                ParameterizedProducerRecord.<AzureGroupMembership>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(object.id)
                        .value(object)
                        .build()
        );
    }
}
