package no.fintlabs;

import com.azure.core.http.rest.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.http.BaseCollectionResponse;
import com.microsoft.graph.http.GraphErrorResponse;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.*;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.ResourceGroupMembership;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.matchers.Null;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.graph.http.GraphError;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        //when(config.getEntobjectid()).thenReturn("testentobjectid");

        ResourceGroup resourceGroup = ResourceGroup.builder()
                .id("12")
                .resourceId("123")
                .displayName("testdisplayname")
                .identityProviderGroupObjectId("testidpgroup")
                .resourceName("testresourcename")
                .resourceType("testresourcetype")
                .resourceLimit("1000")
                .build();

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
        // TODO: Implement test [FKS-187]
    }

    @Mock
    DirectoryObjectCollectionWithReferencesRequestBuilder directoryObjectCollectionWithReferencesRequestBuilder;

    @Mock
    private DirectoryObjectCollectionReferenceRequestBuilder directoryObjectCollectionReferenceRequestBuilder;
    @Mock
    private DirectoryObjectCollectionReferenceRequest directoryObjectCollectionReferenceRequest;

    @Test
    public void makeSureEmptyReferencesIsHandledCorrectly () {
        when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
        when(groupRequestBuilder.members()).thenReturn(directoryObjectCollectionWithReferencesRequestBuilder);
        when(directoryObjectCollectionWithReferencesRequestBuilder.references()).thenReturn(null);


        String kafkaKey = "somekey";
        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        assertThrows(   NullPointerException.class,
                        () -> azureClient.addGroupMembership(resourceGroupMembership, kafkaKey)
        );
    }

    @Test
    public  void addGroupMembership () {
        when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
        when(groupRequestBuilder.members()).thenReturn(directoryObjectCollectionWithReferencesRequestBuilder);
        when(directoryObjectCollectionWithReferencesRequestBuilder.references()).thenReturn(directoryObjectCollectionReferenceRequestBuilder);
        when(directoryObjectCollectionWithReferencesRequestBuilder.references().buildRequest()).thenReturn(directoryObjectCollectionReferenceRequest);

        String kafkaKey = "somekey";
        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        azureClient.addGroupMembership(resourceGroupMembership, kafkaKey);

        verify(directoryObjectCollectionReferenceRequest, times(1)).post(any(DirectoryObject.class));

    }

    @Test
    public void makeSureHTTP400IsHandledGraceullyWhenAddingGroupMembership () {
        // TODO: Make sure HTTP 400 is handled gracefully [FKS-213]
        /*when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
        when(groupRequestBuilder.members()).thenReturn(directoryObjectCollectionWithReferencesRequestBuilder);
        when(directoryObjectCollectionWithReferencesRequestBuilder.references()).thenReturn(directoryObjectCollectionReferenceRequestBuilder);
        when(directoryObjectCollectionWithReferencesRequestBuilder.references().buildRequest()).thenReturn(directoryObjectCollectionReferenceRequest);

        ObjectMapper objectMapper = new ObjectMapper();

        String jsonResponse = "{ \"error\": { \"code\": \"InternalServerError\", \"message\": \"Internal Server Error\" } }";
        GraphErrorResponse errorResponse = objectMapper.readValue(jsonResponse, GraphErrorResponse.class);

        GraphServiceException graphServiceException = new GraphServiceException().
        /*GraphErrorResponse errorResponse = new GraphErrorResponse();
        errorResponse.setRawObject(
                GraphErrorResponse.buildFromJson()
        );*/

        /*aphServiceException graphServiceException = new GraphServiceException(
                "Error Message",
                "Error Description",
                Arrays.asList("TestHeader: testValue"),
                "RequestId",
                500, "" +
                "Error Subcode",
                Arrays.asList("TestRetHeaderr: testRetValue"),
                errorResponse
                true);*/



        /*when(directoryObjectCollectionReferenceRequest.post()).thenThrow(graphServiceException);



        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();*/

        /*GraphServiceException graphServiceException = GraphServiceException.createFromResponse(
                "ExampleMSGraphURL",
                "GET",
                Arrays.asList("exampleRequestHeaders"),
                "exampleRequestBody",
                Map.of("exampleHeader", "exampleHeaderValue" ),
                "exampleResponseMessage",
                400,
                new GraphErrorResponse(),
                true
        );*/

        /*assertThrows(GraphServiceException.class,
                ()->azureClient.addGroupMembership(resourceGroupMembership,"exampleKey"));*/

/*        when(directoryObjectCollectionReferenceRequest.post()).thenThrow(graphServiceException);

        String kafkaKey = "somekey";
        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        assertThrows(   NullPointerException.class,
                () -> azureClient.addGroupMembership(resourceGroupMembership, kafkaKey)
        );*/
    }

    @Mock
    private DirectoryObjectWithReferenceRequestBuilder directoryObjectWithReferenceRequestBuilder;
    @Mock
    private DirectoryObjectReferenceRequestBuilder directoryObjectReferenceRequestBuilder;

    @Mock
    private DirectoryObjectReferenceRequest directoryObjectReferenceRequest;

    @Test
    public void gracefullyHandleErroneousKafkaID () {

    }
    @Test
    public void makeSureAzureDeleteFunctionIsCalled () {
        when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
        when(groupRequestBuilder.members(anyString())).thenReturn(directoryObjectWithReferenceRequestBuilder);
        when(directoryObjectWithReferenceRequestBuilder.reference()).thenReturn(directoryObjectReferenceRequestBuilder);
        when(directoryObjectReferenceRequestBuilder.buildRequest()).thenReturn(directoryObjectReferenceRequest);

        DirectoryObject directoryObject = new DirectoryObject();
        when(directoryObjectReferenceRequest.delete()).thenReturn(new DirectoryObject());

        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        String kafkaKey = "exampleGroupID_exampleUserID";

        azureClient.deleteGroupMembership(resourceGroupMembership, kafkaKey);

        verify(directoryObjectReferenceRequest, times(1) ).delete();
    }

    @Test
    public void makeSureExternalAttributeNullIsSupported() {

    }
    @Test
    public void logAndSkipDeletionWhenKafkaIDIswithoutUnderscore () {
        when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
        when(groupRequestBuilder.members(anyString())).thenReturn(directoryObjectWithReferenceRequestBuilder);
        when(directoryObjectWithReferenceRequestBuilder.reference()).thenReturn(directoryObjectReferenceRequestBuilder);
        when(directoryObjectReferenceRequestBuilder.buildRequest()).thenReturn(directoryObjectReferenceRequest);

        when(directoryObjectReferenceRequest.delete()).thenReturn(new DirectoryObject());

        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        String kafkaKey = "example";
        azureClient.deleteGroupMembership(resourceGroupMembership, kafkaKey);
        verify(directoryObjectReferenceRequest, times(0)).delete();

        kafkaKey = "exampleGroupID_exampleUserID";
        azureClient.deleteGroupMembership(resourceGroupMembership, kafkaKey);
        verify(directoryObjectReferenceRequest, times(1)).delete();
    }

    @Test
    public void logAndSkipDeletionWhenKafkaIDhaveMultipleUnderscores () {
        when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
        when(groupRequestBuilder.members(anyString())).thenReturn(directoryObjectWithReferenceRequestBuilder);
        when(directoryObjectWithReferenceRequestBuilder.reference()).thenReturn(directoryObjectReferenceRequestBuilder);
        when(directoryObjectReferenceRequestBuilder.buildRequest()).thenReturn(directoryObjectReferenceRequest);

        when(directoryObjectReferenceRequest.delete()).thenReturn(new DirectoryObject());

        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        String kafkaKey = "example_with_multiple_underscores";
        azureClient.deleteGroupMembership(resourceGroupMembership, kafkaKey);
        verify(directoryObjectReferenceRequest, times(0)).delete();

        kafkaKey = "exampleGroupID_exampleUserID";
        azureClient.deleteGroupMembership(resourceGroupMembership, kafkaKey);
        verify(directoryObjectReferenceRequest, times(1)).delete();

        kafkaKey = "exampleGroupID_exampleUserID2";
        azureClient.deleteGroupMembership(resourceGroupMembership, kafkaKey);
        verify(directoryObjectReferenceRequest, times(2)).delete();
    }


}