package no.fintlabs;

import no.fintlabs.kafka.ResourceGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AzureClientTest {

    @Test
    void doesGroupExist() {
        String resourceGroupID = "testresourcegroup";
        ResourceGroup resourceGroup = new ResourceGroup(resourceGroupID, "testresource", "testDisplayname", "testidp", "testresourcename", "testresourcetype", "testresourcelimit");

        when(azureClient.doesGroupExist(resourceGroupID)).thenReturn(false);

        resourceGroupConsumerService.processEntity(resourceGroup, null);
        verify(azureClient, times(1));

        // asserts (hvis returnvalue)
        // verify (mockito)
        // Kan også verifisere input til kall. Sjekk parameterverdien er innafor <a,b> osv

    }

    @Test
    void addGroupToAzure() {
    }

    @Test
    void deleteGroup() {
    }

    @Test
    void updateGroup() {
    }
}