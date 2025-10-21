package no.fintlabs.azure;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.model.ParameterizedProducerRecord;
import no.fintlabs.kafka.producing.ParameterizedTemplate;
import no.fintlabs.kafka.producing.ParameterizedTemplateFactory;
import no.fintlabs.kafka.topic.EntityTopicService;
import no.fintlabs.kafka.topic.configuration.CleanupFrequency;
import no.fintlabs.kafka.topic.configuration.EntityTopicConfiguration;
import no.fintlabs.kafka.topic.name.EntityTopicNameParameters;
import no.fintlabs.kafka.topic.name.TopicNamePrefixParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AzureGroupMembershipProducerService {

    private final ParameterizedTemplate<AzureGroupMembership> azureGroupMembershipTemplate;
    private final EntityTopicNameParameters entityTopicNameParameters;
    private final ExecutorService senderExec =
            new ThreadPoolExecutor(
                    1, 1,
                    0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(50_000),
                    r -> { Thread t = new Thread(r, "membership-sender"); t.setDaemon(true); return t; },
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

    public AzureGroupMembershipProducerService(
            ParameterizedTemplateFactory parameterizedTemplateFactory,
            EntityTopicService entityTopicService
    ) {
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

    public void addMembership(AzureGroupMembership azureMembership) {
        senderExec.execute(() -> publishAddedMembership(azureMembership));
    }

    public void removeMembership(AzureGroupMembership azureMembership) {
        senderExec.execute(() -> publishDeletedMembership(azureMembership.getId()));
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
