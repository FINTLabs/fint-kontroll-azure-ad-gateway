 package no.fintlabs;

import com.azure.core.http.rest.PagedResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.directoryobjects.item.DirectoryObjectItemRequestBuilder;
import com.microsoft.graph.education.classes.item.group.GroupRequestBuilder;
import com.microsoft.graph.groups.GroupsRequestBuilder;
import com.microsoft.graph.groups.item.GroupItemRequestBuilder;
import com.microsoft.graph.groups.item.getmemberobjects.GetMemberObjectsRequestBuilder;
import com.microsoft.graph.groups.item.owners.graphserviceprincipal.GraphServicePrincipalRequestBuilder;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.kiota.RequestAdapter;
import lombok.RequiredArgsConstructor;
import no.fintlabs.azure.AzureGroupMembership;
import no.fintlabs.azure.AzureGroupMembershipProducerService;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.ResourceGroupMembership;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.InterruptedIOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@ExtendWith(MockitoExtension.class)
class AzureClientTest {



    @Mock
    private GraphServiceClient graphServiceClient;
    @Mock
    private GroupCollectionResponse groupCollectionPage;
    @Mock
    private Request groupCollectionRequest;

    @Mock
    private CompletableFuture<GroupCollectionResponse> groupCollectionPageFuture;

    @Mock
    private GroupCollectionResponse groupCollectionResponse;

    @Mock
    private GroupItemRequestBuilder groupItemRequestBuilder;

    @Mock
    private GroupsRequestBuilder groupsRequestBuilder;

    @Mock
    private GroupRequestBuilder groupRequestBuilder;

    @Mock
    private GroupsRequestBuilder groupRequestConfiguration;

    @Mock
    private ConfigGroup configGroup;

//    @Mock
//    private ConfigGroup configGroup;

    @Mock
    private ConfigUser configUser;

    @Mock
    private Request groupRequest;

    @Mock
    Group group;

    @Mock
    private Config config;


    @InjectMocks
    private AzureClient azureClient;

    /*@BeforeEach
    void setUp() {
     MockitoAnnotations.openMocks(this);           // Initialize the mocks

        }*/

    private ResourceGroup resourceGroup(){
        // Create a mock ResourceGroup object
        ResourceGroup resourceGroup = ResourceGroup.builder()
                .id("12")
                .resourceId("123")
                .displayName("testdisplayname")
                .identityProviderGroupObjectId("testidpgroup")
                .resourceName("testresourcename")
                .resourceType("testresourcetype")
                .resourceLimit("1000")
                .build();
        return resourceGroup;
    }

    private Group azureGroupObject()
    {
        Group group = new Group();
        group.setDisplayName("testgroup1");
        group.setId("098393-7593-8754-93875-4983754");
        HashMap<String, Object> additionalData = new HashMap<>();
        additionalData.put(configGroup.getFintkontrollidattribute(), "123");
        group.setAdditionalData(additionalData);
        return group;
    }

    private List<Group> getTestGrouplist(int numberOfGroups) {
        ConfigGroup configGroup = new ConfigGroup();
        List<Group> retGroupList = new ArrayList<>();
        for (int i=0; i<numberOfGroups; i++) {
            Group group = new Group();
            group.setId("12" + (i + 2));
            group.setDisplayName("testgroup1");
            HashMap<String, Object> additionalData = new HashMap<>();
            additionalData.put(configGroup.getFintkontrollidattribute(), "123");
            group.setAdditionalData(additionalData);
            retGroupList.add(group);
        }

        return retGroupList;
    }
    @Test
    void doesGroupExist_found() throws Exception {
        String resourceGroupID = "123";

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.get(any())).thenReturn(groupCollectionResponse);

        List<Group> groupList = getTestGrouplist(3);
        when(groupCollectionResponse.getValue()).thenReturn(groupList);

        assertTrue(azureClient.doesGroupExist(resourceGroupID));
    }
    @Test
    void doesGroupExist_notfound() throws Exception {
        String resourceGroupID = "234";

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.get(any())).thenReturn(groupCollectionResponse);

        List<Group> groupList = getTestGrouplist(3);
        when(groupCollectionResponse.getValue()).thenReturn(groupList);

        assertFalse(azureClient.doesGroupExist(resourceGroupID));
    }

    @Test
    void doesGroupExist_throwswhennextpageisindicated() {
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.get(any())).thenReturn(groupCollectionResponse);

        when(groupCollectionResponse.getOdataNextLink()).thenReturn("somefakeurl");

        assertThrows(Exception.class,
                () -> azureClient.doesGroupExist("123")
        );

    }

     @Test
     void addGroupToAzure() {

         // Create a mock group and the future it should return
         Group mockGroup = new Group();
         CompletableFuture<Group> future = CompletableFuture.completedFuture(mockGroup);

         // Mock the graph service client
         GraphServiceClient graphServiceClient = mock(GraphServiceClient.class);
         when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);

         // Call the method under test
         this.azureClient.addGroupToAzure(resourceGroup());

         // Verify that patchAsync was called with the correct parameters
         verify(groupCollectionRequest, times(1));
     }

/*

     @Test
     void deleteGroup() {
         String delGroupID = "123";
         String mockGroupId = "mock-group-id";

         // Step 1: Mock the group object that will be returned by the get() method
         Group mockGroup = new Group();  // Assuming Group is the model you expect
         mockGroup.setId(mockGroupId);

         // Step 2: Mock the GraphServiceClient using its builder pattern
         when(this.graphServiceClient.getRequestAdapter()).thenReturn(mock(RequestAdapter.class));
         GraphServiceClient graphServiceClient = mock(GraphServiceClient.class);
         GroupsRequestBuilder groupsRequestBuilder = mock(GroupsRequestBuilder.class);
         GroupItemRequestBuilder groupItemRequestBuilder = mock(GroupItemRequestBuilder.class);


         // Configure the mock to return the GroupsRequestBuilder
         when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);

         // Configure the mock to return the GroupItemRequestBuilder
         when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);

         // Configure the mock to return the Group object
         when(groupItemRequestBuilder.get()).thenReturn(mockGroup);

         // Assuming `azureClient` uses `graphServiceClient` internally
         // Ensure that `azureClient` is configured to use the mocked `graphServiceClient`
         graphServiceClient.setRequestAdapter(mock(RequestAdapter.class));
         //azureClient.setGraphServiceClient(graphServiceClient); // Adjust this line based on your actual setup


         // Call your deleteGroup method (assume it uses graphServiceClient internally)
         azureClient.deleteGroup(delGroupID);

         // Verify that the get() method was called on groupItemRequestBuilder
         verify(groupItemRequestBuilder, times(1)).get();
     }


     @Test
     void updateGroup() {
         // Prepare a mock Group object
         Group mockGroup = new Group();

         // Create a mock ResourceGroup object
         ResourceGroup resourceGroup = ResourceGroup.builder()
                 .id("12")
                 .resourceId("123")
                 .displayName("testdisplayname")
                 .identityProviderGroupObjectId("testidpgroup")
                 .resourceName("testresourcename")
                 .resourceType("testresourcetype")
                 .resourceLimit("1000")
                 .build();

         // Mock the GroupRequestBuilder and GroupRequest
         GroupRequestBuilder groupRequestBuilder = mock(GroupRequestBuilder.class);
         GroupRequest groupRequest = mock(GroupRequest.class);

         // Mock the behavior of the graphServiceClient
         when(graphServiceClient.groups(resourceGroup.getIdentityProviderGroupObjectId())).thenReturn(groupRequestBuilder);
         when(groupRequestBuilder.buildRequest()).thenReturn(groupRequest);

         // Mock the behavior of patchAsync to return a completed CompletableFuture
         CompletableFuture<Group> future = CompletableFuture.completedFuture(mockGroup);
         when(groupRequest.patchAsync(any(Group.class))).thenReturn(future);

         // Call the method under test
         azureClient.updateGroup(resourceGroup);

         // Verify that patchAsync was called once with any Group object
         verify(groupRequest, times(1)).patchAsync(any(Group.class));
     }

     @Test
     void makeSureUpdateGroupIsCalled() {

         // Mocking GroupRequestBuilder and GroupRequest
         when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
         when(groupRequestBuilder.buildRequest()).thenReturn(groupRequest);

         // Creating a mock ResourceGroup object
         ResourceGroup resourceGroup = ResourceGroup.builder()
                 .id("12")
                 .resourceId("123")
                 .displayName("testdisplayname")
                 .identityProviderGroupObjectId("testidpgroup")
                 .resourceName("testresourcename")
                 .resourceType("testresourcetype")
                 .resourceLimit("1000")
                 .build();

         // Mocking the behavior of patchAsync to return a completed future
         CompletableFuture<Group> future = CompletableFuture.completedFuture(new Group());
         when(groupRequest.patchAsync(any(Group.class))).thenReturn(future);

         // Call the method under test
         azureClient.updateGroup(resourceGroup);

         // Verify that patchAsync is called exactly once with any Group object
         verify(groupRequest, times(1)).patchAsync(any(Group.class));

         // Verify that postAsync and deleteAsync are not called
         verify(groupRequest, times(0)).postAsync(any());
         verify(groupRequest, times(0)).deleteAsync();

         // TODO: Implement further tests [FKS-187]
     }


    @Mock

    DirectoryObjectItemRequestBuilder directoryObjectCollectionWithReferencesRequestBuilder;

    @Mock
    private DirectoryObjectItemRequestBuilder directoryObjectCollectionReferenceRequestBuilder;
    @Mock
    private DirectoryObjectItemRequestBuilder directoryObjectCollectionReferenceRequest;

     @Mock
     private AzureGroupMembershipProducerService azureGroupMembershipProducerService;

     @Test
     public void makeSureEmptyReferencesIsHandledCorrectly() {
         when(graphServiceClient.groups().byGroupId(anyString())).thenReturn(groupItemRequestBuilder);

         PagedResponse<DirectoryObject> emptyPagedResponse = mock(PagedResponse.class);
         when(emptyPagedResponse.getValue()).thenReturn(Collections.emptyList());

         CompletableFuture<PagedResponse<DirectoryObject>> futurePagedResponse = CompletableFuture.completedFuture(emptyPagedResponse);
         when(groupItemRequestBuilder.getMemberObjects()).thenReturn(isNull());

         // Set up the input object for the test
         String kafkaKey = "somekey";
         ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                 .id("testid")
                 .azureGroupRef("exampleGroupRef")
                 .azureUserRef("someUserRef")
                 .roleRef("exampleRoleRef")
                 .build();

         // Assert that NullPointerException is thrown when adding group membership with null references.
         assertThrows(NullPointerException.class,
                 () -> azureClient.addGroupMembership(resourceGroupMembership, kafkaKey)
         );

     }


         @Test
     public void addGroupMembership() {
         // Setup the mocks
         when(graphServiceClient.groups(anyString())).thenReturn(groupRequestBuilder);
         when(groupRequestBuilder.members()).thenReturn(directoryObjectCollectionWithReferencesRequestBuilder);
         when(directoryObjectCollectionWithReferencesRequestBuilder.references()).thenReturn(directoryObjectCollectionReferenceRequestBuilder);
         when(directoryObjectCollectionReferenceRequestBuilder.buildRequest()).thenReturn(directoryObjectCollectionReferenceRequest);

         // Create a mock DirectoryObject
         DirectoryObject mockDirectoryObject = new DirectoryObject();
         mockDirectoryObject.id = "someUserRef";

         // Mocking postAsync to return a completed CompletableFuture with DirectoryObject
         CompletableFuture<DirectoryObject> future = CompletableFuture.completedFuture(mockDirectoryObject);
         when(directoryObjectCollectionReferenceRequest.postAsync(any(DirectoryObject.class))).thenReturn(future);

         // Creating test data
         String kafkaKey = "somekey";
         ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                 .id("testid")
                 .azureGroupRef("exampleGroupRef")
                 .azureUserRef("someUserRef")
                 .roleRef("exampleRoleRef")
                 .build();

         // Call the method under test
         azureClient.addGroupMembership(resourceGroupMembership, kafkaKey);

         // Verify the interaction
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
        );
    }

    @Mock
    private DirectoryObjectItemRequestBuilder directoryObjectWithReferenceRequestBuilder;
    @Mock
    private DirectoryObjectItemRequestBuilder directoryObjectReferenceRequestBuilder;

    @Mock
    private DirectoryObjectItemRequestBuilder directoryObjectReferenceRequest;

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

        azureClient.deleteGroupMembership(kafkaKey);

        verify(directoryObjectReferenceRequest, times(1) ).deleteAsync();
    }

    // @Test
    //public void makeSureNulluser

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
        azureClient.deleteGroupMembership(kafkaKey);
        verify(directoryObjectReferenceRequest, times(0)).deleteAsync();

        kafkaKey = "exampleGroupID_exampleUserID";
        azureClient.deleteGroupMembership(kafkaKey);
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
        azureClient.deleteGroupMembership(membershipkey);

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
        azureClient.deleteGroupMembership(kafkaKey);
        verify(directoryObjectReferenceRequest, times(0)).deleteAsync();

        kafkaKey = "exampleGroupID_exampleUserID";
        azureClient.deleteGroupMembership(kafkaKey);
        verify(directoryObjectReferenceRequest, times(1)).deleteAsync();

        kafkaKey = "exampleGroupID_exampleUserID2";
        azureClient.deleteGroupMembership(kafkaKey);
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
        when(graphServiceClient.groups()).thenReturn(groupItemRequestBuilder);
        when(groupItemRequestBuilder.buildRequest()).thenReturn(groupCollectionRequest);
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
        when(graphServiceClient.groups()).thenReturn(groupItemRequestBuilder);
        //when(groupCollectionRequestBuilder.buildRequest(any(LinkedList.class))).thenReturn(groupCollectionRequest);
        when(groupItemRequestBuilder.buildRequest()).thenReturn(groupCollectionRequest);
        when(groupCollectionRequest.select(anyString())).thenReturn(groupCollectionRequest);
       // when(groupCollectionRequest.expand(anyString())).thenReturn(groupCollectionRequest);
        //when(groupCollectionRequest.filter(anyString())).thenReturn(groupCollectionRequest);

        when(groupCollectionRequest.getAsync()).thenThrow(new ClientException("Timeout", new InterruptedIOException("timeout")));

        /*GroupCollectionRequestBuilder mockGroupCollectionRequestBuilder2 = Mockito.mock(GroupCollectionRequestBuilder.class);
        GroupCollectionRequest mockGroupCollectionRequest2 = Mockito.mock(GroupCollectionRequest.class);
        GroupCollectionPage mockCollPage2= Mockito.mock(GroupCollectionPage.class);*/

        /*when(groupCollectionPage.getNextPage()).thenReturn(mockGroupCollectionRequestBuilder2);
        when(mockGroupCollectionRequestBuilder2.buildRequest()).thenReturn(mockGroupCollectionRequest2);
        when(mockGroupCollectionRequest2.get()).thenReturn(mockCollPage2);

        azureClient.pullAllGroups();

        when(groupCollectionRequest.get()).thenAnswer();

        azureClient.pullAllGroups();*/

        /*verify(groupCollectionPage, times(2)).getNextPage();
        verify(mockCollPage2, times(1)).getNextPage();
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
     */

}

