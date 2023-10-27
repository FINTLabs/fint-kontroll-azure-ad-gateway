package no.fintlabs;

import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.GroupCollectionRequest;
import com.microsoft.graph.requests.GroupCollectionRequestBuilder;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.kafka.ResourceGroup;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AzureClientTest {
    @Mock
    private GraphServiceClient<Request> graphServiceClient;
    @Mock
    private GroupCollectionPage groupCollectionPage;
    @Mock
    private GroupCollectionRequest groupCollectionRequest;
    @Mock
    private GroupCollectionRequestBuilder groupCollectionRequestBuilder;
    @Mock ConfigGroup configGroup;
    @Mock Group group;

    @InjectMocks
    private AzureClient azureClient;

    @Test
    void doesGroupExist_found() {
        String resourceGroupID = "123";
        //when(azureClient.doesGroupExist(resourceGroupID)).thenReturn(false);


        when(graphServiceClient.groups()).thenReturn(groupCollectionRequestBuilder);
        when(graphServiceClient.groups().buildRequest()).thenReturn(groupCollectionRequest);
        when(graphServiceClient.groups().buildRequest().select(anyString())).thenReturn(groupCollectionRequest);
        when(graphServiceClient.groups().buildRequest().get()).thenReturn(groupCollectionPage);


        List<Group> groupList = new ArrayList<Group>();
        Group group = new Group();
        group.displayName = "testgroup1";
        groupList.add(group);
        group.displayName = "testgroup2";
        groupList.add(group);
        when(groupCollectionPage.getCurrentPage()).thenReturn(groupList);

        boolean checkvar = azureClient.doesGroupExist(resourceGroupID);
        assertTrue(checkvar);
    }
    @Test
    void doesGroupExist_notfound() {

    }
    @Test
    void doesGroupExist() {
        String resourceGroupID = "12";
        //ResourceGroup resourceGroup = new ResourceGroup(resourceGroupID, "testresource", "testDisplayname", "testidp", "testresourcename", "testresourcetype", "testresourcelimit");
        //doesGroupExist(resourceGroupID);
        //azureClient.doesGroupExist("");
        //String resourceGroupID = "testresourcegroup";
        //ResourceGroup resourceGroup = new ResourceGroup(resourceGroupID, "testresource", "testDisplayname", "testidp", "testresourcename", "testresourcetype", "testresourcelimit");

        //when(azureClient.doesGroupExist(resourceGroupID)).thenReturn(false);

        //resourceGroupConsumerService.processEntity(resourceGroup, null);
        //verify(azureClient, times(1));

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