package no.fintlabs.kafka;

import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.ResourceReference;
import no.fintlabs.AzureClient;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Service
@ExtendWith(MockitoExtension.class)
class ResourceGroupMembershipConsumerServiceTest {

    @Mock
    private AzureClient azureClient;
    @InjectMocks
    private ResourceGroupMembershipConsumerService resourceGroupMembershipConsumerService;

    @Mock
    private EntityTopicService entityTopicService;

    static private ResourceGroupMembership exampleGroupMembership;

    @BeforeAll()
    static void setUpFirst() {
        exampleGroupMembership = ResourceGroupMembership.builder()
                .id("exampleID")
                .azureUserRef("exampleUserRef")
                .azureGroupRef("exampleGroupRef")
                .roleRef("exampleRole")
                .build();
    }

    @Test
    void handleNonExistingKafkaQueue() {

    }

    @Test
    void processEntityNewGroupmemberhipDetected() {

        ResourceGroupMembership resourceGroupMembership = exampleGroupMembership.toBuilder()
                .id(null)
                .build();

        resourceGroupMembershipConsumerService.processEntity(resourceGroupMembership, "exampleID");

        verify(azureClient, times(1)).addGroupMembership(any(ResourceGroupMembership.class), anyString());
        verify(azureClient, times(0)).deleteGroupMembership(any(ResourceGroupMembership.class), anyString());
    }

    @Test
    void processEntityDeleteGroupmemberhip() {
        resourceGroupMembershipConsumerService.processEntity(exampleGroupMembership, "exampleID");

        verify(azureClient, times(0)).addGroupMembership(any(ResourceGroupMembership.class), anyString());
        verify(azureClient, times(1)).deleteGroupMembership(any(ResourceGroupMembership.class), anyString());
    }
}