package no.fintlabs.kafka;

import com.microsoft.graph.models.ResourceReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.stereotype.Service;

import static org.junit.jupiter.api.Assertions.*;
@Service

class ResourceGroupMembershipConsumerServiceTest {

    @Mock
    private ResourceGroupConsumerService resourceGroupConsumerService;
    private ResourceGroupMembership resourceGroupMembership;
    private String resourceGroupMembershipKey;
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

    @BeforeEach
    void setUp() {
        resourceGroupMembership = ResourceGroupMembership.builder()
                .azureGroupRef("fakeAzureGroup")
                .azureUserRef("fakeUserUUID")
                .roleRef("someRoleRef")
                .build();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void processEntityNewGroupmemberhipDetected() {
        // TODO: Implement group membership test [FKS-112]
    }

    @Test
    void processEntityUpdateGroupmemberhip() {
        // TODO: Implement group membership test [FKS-112]
    }

    @Test
    void processEntityRemoveGroupMembership() {
        // TODO: Implement removal of group membership test [FKS-212]
    }
}