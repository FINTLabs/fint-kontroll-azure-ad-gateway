package no.fintlabs.kafka;

import com.microsoft.graph.core.BaseClient;
import com.microsoft.graph.core.IBaseClient;
import com.microsoft.graph.requests.GraphServiceClient;
import jakarta.annotation.Resource;
import no.fintlabs.AzureClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceGroupConsumerServiceTest {

    @Mock
    private AzureClient azureClient;

    @InjectMocks
    private ResourceGroupConsumerService resourceGroupConsumerService;

    /*@Test
    void doesGroupExist() {
    }*/

    @Test
    void processEntity() {
        /*when(graphServiceClient.groups()).thenReturn(
                new com.microsoft.graph.requests.GroupCollectionRequestBuilder(
                        "requestURL",
                        new BaseClient<>()
                )
        ) {
        }))*/
        //resourceGroupConsumerService = new ResourceGroupConsumerService(null, null,null, null);'
        String resourceGroupID = "testresourcegroup";
        ResourceGroup resourceGroup = new ResourceGroup(resourceGroupID, "testresource", "testDisplayname", "testidp", "testresourcename", "testresourcetype", "testresourcelimit");

        when(azureClient.doesGroupExist(resourceGroupID)).thenReturn(false);

        resourceGroupConsumerService.processEntity(resourceGroup, null);

        // asserts (hvis returnvalue)
        // verify (mockito)
        // Kan også verifisere input til kall. Sjekk parameterverdien er innafor <a,b> osv

    }
}