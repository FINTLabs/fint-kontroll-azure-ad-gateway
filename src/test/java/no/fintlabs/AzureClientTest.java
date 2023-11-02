package no.fintlabs;

import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.*;
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
    @Mock
    private ConfigGroup configGroup;
    //@Mock Group group;

    @Mock
    private Config config;

    @InjectMocks
    private AzureClient azureClient;
    private List<Group> getTestGrouplist(int numberOfGroups) {
        List<Group> retGroupList = new ArrayList<>();
        for (int i=0; i<numberOfGroups; i++) {
            Group group = new Group();
            group.id = "12" + (i + 2);
            group.displayName = "testgroup1";
            group.additionalDataManager().put(configGroup.getFintkontrollidattribute(), new JsonPrimitive("123"));
            retGroupList.add(group);
        }

        return retGroupList;
    }
    @Test
    void doesGroupExist_found() {
        String resourceGroupID = "123";

        when(graphServiceClient.groups()).thenReturn(groupCollectionRequestBuilder);
        when(graphServiceClient.groups().buildRequest()).thenReturn(groupCollectionRequest);
        when(graphServiceClient.groups().buildRequest().select(anyString())).thenReturn(groupCollectionRequest);
        when(graphServiceClient.groups().buildRequest().get()).thenReturn(groupCollectionPage);

        List<Group> groupList = getTestGrouplist(3);

        when(groupCollectionPage.getCurrentPage()).thenReturn(groupList);

        boolean checkvar = azureClient.doesGroupExist(resourceGroupID);

        assertTrue(checkvar);
    }
    @Test
    void doesGroupExist_notfound() {
        String resourceGroupID = "234";

        when(graphServiceClient.groups()).thenReturn(groupCollectionRequestBuilder);
        when(graphServiceClient.groups().buildRequest()).thenReturn(groupCollectionRequest);
        when(graphServiceClient.groups().buildRequest().select(anyString())).thenReturn(groupCollectionRequest);
        when(graphServiceClient.groups().buildRequest().get()).thenReturn(groupCollectionPage);

        List<Group> groupList = getTestGrouplist(3);
        when(groupCollectionPage.getCurrentPage()).thenReturn(groupList);


        assertFalse(azureClient.doesGroupExist(resourceGroupID));

    }

    @Test
    void addGroupToAzure() {
        when(graphServiceClient.groups()).thenReturn(groupCollectionRequestBuilder);
        when(graphServiceClient.groups().buildRequest()).thenReturn(groupCollectionRequest);

        ResourceGroup resourceGroup = new ResourceGroup("12", "123", "testdisplayname", "testidpgroup", "testresourcename", "testresourcetype", "1000");
        azureClient.addGroupToAzure(resourceGroup);

        verify(groupCollectionRequest, times(1)).post(any(Group.class));
    }

    @Mock
    private GroupRequestBuilder groupRequestBuilder;
    @Mock
    private GroupRequest groupRequest;

    @Test
    void deleteGroup() {
        String delGroupID = "123";
        when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
        when(groupRequestBuilder.buildRequest()).thenReturn(groupRequest);

        azureClient.deleteGroup(delGroupID);

        verify(groupRequest, times(1)).delete();
    }

    @Test
    void updateGroup() {
    }
}