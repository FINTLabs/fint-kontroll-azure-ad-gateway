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

public class AzureGroupMembershipProducerService
{
    private final EntityProducer<AzureGroupMembership> entityProducer;
    private final EntityTopicNameParameters entityTopicNameParameters;

    public AzureGroupMembershipProducerService(
            EntityTopicService entityTopicService,
            EntityProducerFactory entityProducerFactory) {

        entityProducer = entityProducerFactory.createProducer(AzureGroupMembership.class);
        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("azuread-resource-group-membership")
                .build();
        entityTopicService.ensureTopic(entityTopicNameParameters,0);
    }
    public void publishDeletedMembership(String membershipKey) {
        entityProducer.send(
                EntityProducerRecord.<AzureGroupMembership>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(membershipKey)
                        .value(null)
                        .build()
        );
    }
    public void publishAddedMembership(AzureGroupMembership object) {
        entityProducer.send(
                EntityProducerRecord.<AzureGroupMembership>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(object.id)
                        .value(object)
                        .build()
        );
    }
}
