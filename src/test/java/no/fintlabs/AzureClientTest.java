 package no.fintlabs;

import com.google.gson.JsonPrimitive;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphError;
import com.microsoft.graph.http.GraphErrorResponse;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.requests.*;
import no.fintlabs.azure.AzureGroupMembership;
import no.fintlabs.azure.AzureGroupMembershipProducerService;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.ResourceGroupMembership;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InterruptedIOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

 @ExtendWith(MockitoExtension.class)
class AzureClientTest {
    @Mock
    private GraphServiceClient<Request> graphServiceClient;
    @Mock
    private GroupCollectionPage groupCollectionPage;
    @Mock
    private GroupCollectionRequest groupCollectionRequest;

    @Mock
    private CompletableFuture<GroupCollectionPage> groupCollectionPageFuture;
    @Mock
    private GroupCollectionRequestBuilder groupCollectionRequestBuilder;

    @Mock
    private ConfigGroup configGroup;
    @Mock
    private ConfigUser configUser;
    //@Mock Group group;
    @Mock
    private Config config;

    @InjectMocks
    private AzureClient azureClient;

    private List<Group> getTestGrouplist(int numberOfGroups) {
        ConfigGroup configGroup = new ConfigGroup();
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
        //when(groupCollectionRequest.filter(anyString())).thenReturn(groupCollectionRequest);

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
        //when(groupCollectionRequest.filter(anyString())).thenReturn(groupCollectionRequest);

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

        verify(groupCollectionRequest, times(1)).postAsync(any(Group.class));
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
         when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
         when(groupRequestBuilder.buildRequest()).thenReturn(groupRequest);
         ResourceGroup resourceGroup = ResourceGroup.builder()
                 .id("12")
                 .resourceId("123")
                 .displayName("testdisplayname")
                 .identityProviderGroupObjectId("testidpgroup")
                 .resourceName("testresourcename")
                 .resourceType("testresourcetype")
                 .resourceLimit("1000")
                 .build();
         azureClient.updateGroup(resourceGroup);

         verify(groupRequest, times(1)).patchAsync(any(Group.class));
     }

    @Test
    //void makeSureUpdateGroupIsSkippedWhenConfigparamterUpdateGroupNamesIsFalse() {
    void makeSureUpdateGroupIsCalled() {

        when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
        when(groupRequestBuilder.buildRequest()).thenReturn(groupRequest);

        ResourceGroup resourceGroup = ResourceGroup.builder()
                .id("12")
                .resourceId("123")
                .displayName("testdisplayname")
                .identityProviderGroupObjectId("testidpgroup")
                .resourceName("testresourcename")
                .resourceType("testresourcetype")
                .resourceLimit("1000")
                .build();

        azureClient.updateGroup(resourceGroup);

        verify(groupRequest, times(1)).patchAsync(any(Group.class));
        verify(groupRequest, times(0)).postAsync(any());
        verify(groupRequest, times(0)).deleteAsync();

        // TODO: Implement test [FKS-187]
    }

    @Mock
    DirectoryObjectCollectionWithReferencesRequestBuilder directoryObjectCollectionWithReferencesRequestBuilder;

    @Mock
    private DirectoryObjectCollectionReferenceRequestBuilder directoryObjectCollectionReferenceRequestBuilder;
    @Mock
    private DirectoryObjectCollectionReferenceRequest directoryObjectCollectionReferenceRequest;

     @Mock
     private AzureGroupMembershipProducerService azureGroupMembershipProducerService;

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
        //when(configGroup.getFintkontrollidattribute()).thenReturn("somefakeattribute");
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

        verify(directoryObjectCollectionReferenceRequest, times(1)).postAsync(any(DirectoryObject.class));

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
        //when(directoryObjectReferenceRequest.delete()).thenReturn(new DirectoryObject());

        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        String kafkaKey = "exampleGroupID_exampleUserID";

        azureClient.deleteGroupMembership(resourceGroupMembership, kafkaKey);

        verify(directoryObjectReferenceRequest, times(1) ).deleteAsync();
    }

    /*@Test
    public void makeSureNulluser*/

    @Test
    public void makeSureExternalAttributeNullIsSupported() {

    }
    @Test
    public void logAndSkipDeletionWhenKafkaIDIswithoutUnderscore () {

        when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
        when(groupRequestBuilder.members(anyString())).thenReturn(directoryObjectWithReferenceRequestBuilder);
        when(directoryObjectWithReferenceRequestBuilder.reference()).thenReturn(directoryObjectReferenceRequestBuilder);
        when(directoryObjectReferenceRequestBuilder.buildRequest()).thenReturn(directoryObjectReferenceRequest);

        //when(directoryObjectReferenceRequest.delete()).thenReturn(new DirectoryObject());

        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        String kafkaKey = "example";
        azureClient.deleteGroupMembership(null, kafkaKey);
        verify(directoryObjectReferenceRequest, times(0)).deleteAsync();

        kafkaKey = "exampleGroupID_exampleUserID";
        azureClient.deleteGroupMembership(null, kafkaKey);
        verify(directoryObjectReferenceRequest, times(1)).deleteAsync();
    }

    @Test
    public void makeSureDeleteAzureMembershipCanBeCalledWithNull() {
        when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
        when(groupRequestBuilder.members(anyString())).thenReturn(directoryObjectWithReferenceRequestBuilder);
        when(directoryObjectWithReferenceRequestBuilder.reference()).thenReturn(directoryObjectReferenceRequestBuilder);
        when(directoryObjectReferenceRequestBuilder.buildRequest()).thenReturn(directoryObjectReferenceRequest);

        //when(directoryObjectReferenceRequest.deleteAsync()).thenReturn(new DirectoryObject());

        String membershipkey = "someid_1234";
        azureClient.deleteGroupMembership(null, membershipkey);

        verify(directoryObjectReferenceRequest, times(1)).deleteAsync();
    }
    @Test
    public void logAndSkipDeletionWhenKafkaIDhaveMultipleUnderscores () {
        when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
        when(groupRequestBuilder.members(anyString())).thenReturn(directoryObjectWithReferenceRequestBuilder);
        when(directoryObjectWithReferenceRequestBuilder.reference()).thenReturn(directoryObjectReferenceRequestBuilder);
        when(directoryObjectReferenceRequestBuilder.buildRequest()).thenReturn(directoryObjectReferenceRequest);

        //when(directoryObjectReferenceRequest.delete()).thenReturn(new DirectoryObject());

        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        String kafkaKey = "example_with_multiple_underscores";
        azureClient.deleteGroupMembership(resourceGroupMembership, kafkaKey);
        verify(directoryObjectReferenceRequest, times(0)).deleteAsync();

        kafkaKey = "exampleGroupID_exampleUserID";
        azureClient.deleteGroupMembership(resourceGroupMembership, kafkaKey);
        verify(directoryObjectReferenceRequest, times(1)).deleteAsync();

        kafkaKey = "exampleGroupID_exampleUserID2";
        azureClient.deleteGroupMembership(resourceGroupMembership, kafkaKey);
        verify(directoryObjectReferenceRequest, times(2)).deleteAsync();
    }

    //@MockBean
    //private ConfigGroup configGroup;
//    @Test
//    public void makeSureGetNextPageIsCalledAsExpected() {
//        GroupCollectionRequestBuilder groupCollectionRequestBuilder = mock(GroupCollectionRequestBuilder.class);
//        GroupCollectionRequest groupCollectionRequest = mock(GroupCollectionRequest.class);
//        CompletableFuture<GroupCollectionPage> groupCollectionPageFuture = mock(CompletableFuture.class);
//        GroupCollectionPage groupCollectionPage = mock(GroupCollectionPage.class);
//
//
//        when(graphServiceClient.groups()).thenReturn(groupCollectionRequestBuilder);
//        when(groupCollectionRequestBuilder.buildRequest()).thenReturn(groupCollectionRequest);
//        when(groupCollectionRequest.select(anyString())).thenReturn(groupCollectionRequest);
//        when(groupCollectionRequest.getAsync()).thenReturn(groupCollectionPageFuture);
//
//        GroupCollectionRequestBuilder nextPageRequestBuilder = mock(GroupCollectionRequestBuilder.class);
//        GroupCollectionRequest nextPageRequest = mock(GroupCollectionRequest.class);
//        CompletableFuture<GroupCollectionPage> nextPageFuture = mock(CompletableFuture.class);
//        GroupCollectionPage nextPage = mock(GroupCollectionPage.class);
//
//        when(groupCollectionPageFuture.join()).thenReturn(groupCollectionPage);
//        when(groupCollectionPage.getNextPage()).thenReturn(nextPageRequestBuilder);
//        when(nextPageRequestBuilder.buildRequest()).thenReturn(nextPageRequest);
//        when(nextPageRequest.getAsync()).thenReturn(nextPageFuture);
//        when(nextPageFuture.join()).thenReturn(nextPage);
//
//        azureClient.pullAllGroups();
//
//        //verify(groupCollectionPage, times(1)).getNextPage();
//        //verify(nextPageRequestBuilder, times(1)).buildRequest();
//        //verify(nextPageRequest, times(1)).getAsync();
//        //verify(nextPage, times(1)).getNextPage();
////        when(graphServiceClient.groups()).thenReturn(groupCollectionRequestBuilder);
////        when(groupCollectionRequestBuilder.buildRequest()).thenReturn(groupCollectionRequest);
////        when(groupCollectionRequest.select(anyString())).thenReturn(groupCollectionRequest);
////        //when(groupCollectionRequest.expand(anyString())).thenReturn(groupCollectionRequest);
////        //when(groupCollectionRequest.filter(anyString())).thenReturn(groupCollectionRequest);
////
////        when(groupCollectionRequest.getAsync()).thenReturn(groupCollectionPageFuture);
////
////        GroupCollectionRequestBuilder mockGroupCollectionRequestBuilder2 = Mockito.mock(GroupCollectionRequestBuilder.class);
////        when(groupCollectionPage.getNextPage()).thenReturn(mockGroupCollectionRequestBuilder2);
////        GroupCollectionRequest mockGroupCollectionRequest2 = Mockito.mock(GroupCollectionRequest.class);
////        when(mockGroupCollectionRequestBuilder2.buildRequest()).thenReturn(mockGroupCollectionRequest2);
////        CompletableFuture<GroupCollectionPage> mockCollPage2= mock(CompletableFuture.class);
////        when(mockGroupCollectionRequest2.getAsync()).thenReturn(mockCollPage2);
////
////        azureClient.pullAllGroups();
////
////        verify(groupCollectionPage, times(2)).getNextPage();
////        verify(mockCollPage2, times(1)).getNextPage();
//    }

    @Test
    public void makeSureTrownErrorIsSwallowedAndNotThrown() {
        when(graphServiceClient.groups()).thenReturn(groupCollectionRequestBuilder);
        when(groupCollectionRequestBuilder.buildRequest()).thenReturn(groupCollectionRequest);
        when(groupCollectionRequest.select(anyString())).thenReturn(groupCollectionRequest);
        //when(groupCollectionRequest.expand(anyString())).thenReturn(groupCollectionRequest);
        //when(groupCollectionRequest.filter(anyString())).thenReturn(groupCollectionRequest);

        when(groupCollectionRequest.getAsync()).thenThrow(ClientException.class);

        //assertDoesNotThrow( );
        assertDoesNotThrow(()-> {
            azureClient.pullAllGroups();
        });
    }

    @Test
    public void shouldHandleTimeoutException() {
        when(graphServiceClient.groups()).thenReturn(groupCollectionRequestBuilder);
        //when(groupCollectionRequestBuilder.buildRequest(any(LinkedList.class))).thenReturn(groupCollectionRequest);
        when(groupCollectionRequestBuilder.buildRequest()).thenReturn(groupCollectionRequest);
        when(groupCollectionRequest.select(anyString())).thenReturn(groupCollectionRequest);
       // when(groupCollectionRequest.expand(anyString())).thenReturn(groupCollectionRequest);
        //when(groupCollectionRequest.filter(anyString())).thenReturn(groupCollectionRequest);

        when(groupCollectionRequest.getAsync()).thenThrow(new ClientException("Timeout", new InterruptedIOException("timeout")));

        /*GroupCollectionRequestBuilder mockGroupCollectionRequestBuilder2 = Mockito.mock(GroupCollectionRequestBuilder.class);
        GroupCollectionRequest mockGroupCollectionRequest2 = Mockito.mock(GroupCollectionRequest.class);
        GroupCollectionPage mockCollPage2= Mockito.mock(GroupCollectionPage.class);*/

        /*when(groupCollectionPage.getNextPage()).thenReturn(mockGroupCollectionRequestBuilder2);
        when(mockGroupCollectionRequestBuilder2.buildRequest()).thenReturn(mockGroupCollectionRequest2);
        when(mockGroupCollectionRequest2.get()).thenReturn(mockCollPage2);*/

        azureClient.pullAllGroups();

/*        when(groupCollectionRequest.get()).thenAnswer();

        azureClient.pullAllGroups();*/

        /*verify(groupCollectionPage, times(2)).getNextPage();
        verify(mockCollPage2, times(1)).getNextPage();*/
    }

    @Test
     public void republishAlreadyExistingMembershipToKafka()
         {
             DirectoryObject directoryObject = new DirectoryObject();
             directoryObject.id = "exampleGroupRefNumberID";


             GraphErrorResponse errorResponse = new GraphErrorResponse();
             errorResponse.error = new GraphError();
             errorResponse.error.code = "Request_BadRequest";
             errorResponse.error.message = "object references already exist";

             GraphServiceException graphServiceException = GraphServiceException.createFromResponse(
                     "ExampleMSGraphURL",
                     "POST",
                     Arrays.asList("exampleRequestHeaders"),
                     "exampleRequestBody",
                     Map.of("exampleHeader", "exampleHeaderValue"),
                     "exampleResponseMessage",
                     400,
                     errorResponse,
                     true
             );

             // Set up mocks
             when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
             when(groupRequestBuilder.members()).thenReturn(directoryObjectCollectionWithReferencesRequestBuilder);
             when(directoryObjectCollectionWithReferencesRequestBuilder.references()).thenReturn(directoryObjectCollectionReferenceRequestBuilder);
             when(directoryObjectCollectionWithReferencesRequestBuilder.references().buildRequest()).thenReturn(directoryObjectCollectionReferenceRequest);

             //when(directoryObjectCollectionReferenceRequest.post(any(DirectoryObject.class))).thenReturn(directoryObject);
             when(directoryObjectCollectionReferenceRequest.postAsync(any(DirectoryObject.class))).thenThrow(graphServiceException);
             //when(azureGroupMembershipProducerService).publishAddedMembership();

             String kafkaKey = "somekey";
             ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                     .id("testid")
                     .azureGroupRef("exampleGroupRef")
                     .azureUserRef("someUserRef")
                     .roleRef("exampleRoleRef")
                     .build();

             // Call the method under test
             //try {
             azureClient.addGroupMembership(resourceGroupMembership, kafkaKey);
             //} catch (GraphServiceException e) {
                 // Handle exception as needed or rethrow it
             //    System.out.println("Caught GraphServiceException: " + e.getMessage());
             //}

             // Verify that the post method was called once and threw the exception
             verify(directoryObjectCollectionReferenceRequest, times(1)).postAsync(any(DirectoryObject.class));
             verify(azureGroupMembershipProducerService, times(1)).publishAddedMembership(any(AzureGroupMembership.class));

         }

     @Test
     public void detectBadAzureResourceRefAndLogWarning()
     {
         DirectoryObject directoryObject = new DirectoryObject();
         directoryObject.id = "exampleGroupRefNumberID";

         GraphErrorResponse errorResponse = new GraphErrorResponse();
         errorResponse.error = new GraphError();
         errorResponse.error.code = "Request_ResourceNotFound";
         errorResponse.error.message = "Resource 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx' does not exist or one of its queried reference-property objects are not present.";

         GraphServiceException graphServiceException = GraphServiceException.createFromResponse(
                 "ExampleMSGraphURL",
                 "POST",
                 Arrays.asList("exampleRequestHeaders"),
                 "exampleRequestBody",
                 Map.of("exampleHeader", "exampleHeaderValue"),
                 "exampleResponseMessage",
                 400,
                 errorResponse,
                 true
         );

         // Set up mocks
         when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
         when(groupRequestBuilder.members()).thenReturn(directoryObjectCollectionWithReferencesRequestBuilder);
         when(directoryObjectCollectionWithReferencesRequestBuilder.references()).thenReturn(directoryObjectCollectionReferenceRequestBuilder);
         when(directoryObjectCollectionWithReferencesRequestBuilder.references().buildRequest()).thenReturn(directoryObjectCollectionReferenceRequest);

         //when(directoryObjectCollectionReferenceRequest.post(any(DirectoryObject.class))).thenReturn(directoryObject);
         when(directoryObjectCollectionReferenceRequest.postAsync(any(DirectoryObject.class))).thenThrow(graphServiceException);
         //when(azureGroupMembershipProducerService).publishAddedMembership();

         String kafkaKey = "somekey";
         ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                 .id("testid")
                 .azureGroupRef("exampleGroupRef")
                 .azureUserRef("someUserRef")
                 .roleRef("exampleRoleRef")
                 .build();

         azureClient.addGroupMembership(resourceGroupMembership, kafkaKey);

         // Verify that the post method was called once and threw the exception
         verify(directoryObjectCollectionReferenceRequest, times(1)).postAsync(any(DirectoryObject.class));
         verify(azureGroupMembershipProducerService, times(0)).publishAddedMembership(any(AzureGroupMembership.class));
         //verify(log, times(1))

     }
}

