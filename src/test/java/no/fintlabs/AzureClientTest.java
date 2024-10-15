package no.fintlabs;

//import com.microsoft.graph.directoryobjects.item.DirectoryObjectItemRequestBuilder;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.microsoft.graph.core.tasks.PageIterator;
import com.microsoft.graph.groups.delta.DeltaGetResponse;
import com.microsoft.graph.groups.delta.DeltaRequestBuilder;
import com.microsoft.graph.groups.item.members.item.DirectoryObjectItemRequestBuilder;
import com.microsoft.graph.education.classes.item.group.GroupRequestBuilder;
import com.microsoft.graph.groups.GroupsRequestBuilder;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.RequestAdapter;
import com.microsoft.kiota.serialization.AdditionalDataHolder;
import com.microsoft.kiota.serialization.UntypedArray;
import com.microsoft.kiota.serialization.UntypedNode;
import com.microsoft.kiota.serialization.UntypedObject;
import com.microsoft.graph.models.Entity;
import com.microsoft.graph.groups.item.GroupItemRequestBuilder;
import com.microsoft.graph.groups.item.members.MembersRequestBuilder;
import com.microsoft.graph.groups.item.members.ref.RefRequestBuilder;
//import com.microsoft.graph.requests.GroupCollectionRequest;
//import com.microsoft.graph.requests.GroupCollectionRequestBuilder;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.models.*;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.azure.AzureGroupMembership;
import no.fintlabs.azure.AzureGroupMembershipProducerService;
import no.fintlabs.azure.AzureGroupProducerService;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.ResourceGroupMembership;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Type;
import java.util.*;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;


@ExtendWith(MockitoExtension.class)
class AzureClientTest {

    @Mock
    private GraphServiceClient graphServiceClient;

    @Mock
    private GroupCollectionResponse groupCollectionResponse;

    @Mock
    private GroupsRequestBuilder groupsRequestBuilder;

    @Mock
    private DeltaRequestBuilder deltaRequestBuilder;

    @Mock
    private RequestAdapter requestAdapter;

    @Mock
    private Map<String, Object> map;

    @Mock
    private Object object;

    @Mock
    private UntypedArray untypedArray;

    @Mock
    GroupItemRequestBuilder groupItemRequestBuilder;

    @Mock
    private AzureGroupProducerService azureGroupProducerService;

    @Mock
    private AzureGroupMembershipProducerService azureGroupMembershipProducerService;

    @Mock
    private DirectoryObjectItemRequestBuilder directoryObjectItemRequestBuilder;

    @Mock
    private ConfigGroup configGroup;

    @Mock
    private Group group;

    @Mock
    private AdditionalDataHolder additionalDataHolder;

    @Mock
    private UntypedNode untypedNode;

    @Mock
    private AzureGroup azureGroup;

    @Mock
    private AzureGroupMembership azureGroupMembership;


    @InjectMocks
    private AzureClient azureClient;

    @Mock
    MembersRequestBuilder membersRequestBuilder;

    @Mock
    ApiException apiException;

    @Mock
    RefRequestBuilder refRequestBuilder;

    @Mock
    com.microsoft.graph.groups.item.members.item.ref.RefRequestBuilder singleMemberRefRequestBuilder;

    /*private ResourceGroup resourceGroup(){
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
    }*/

    /*private Group azureGroupObject()
    {
        Group group = new Group();
        group.setDisplayName("testgroup1");
        group.setId("098393-7593-8754-93875-4983754");
        HashMap<String, Object> additionalData = new HashMap<>();
        additionalData.put(configGroup.getFintkontrollidattribute(), "123");
        group.setAdditionalData(additionalData);
        return group;
    }*/

    private List<Group> getTestGrouplist(int numberOfGroups) {
        List<Group> retGroupList = new ArrayList<>();
        for (int i=0; i<numberOfGroups; i++) {
            Group group = new Group();
            group.setId(UUID.randomUUID().toString());
            group.setDisplayName("testgroup" + i + "-suff-");
            HashMap<String, Object> additionalData = new HashMap<>();
            additionalData.put("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId", "123");
            additionalData.put("members@delta", object);
            group.setAdditionalData(additionalData);
//            List<Map<String, Object>> membersDeltaList = new ArrayList<>();
//            Map<String, Object> member1 = new HashMap<>();
//            member1.put("@odata.type", "#microsoft.graph.user");
//            member1.put("id", "693acd06-2877-4339-8ade-b704261fe7a0");
//            Map<String, Object> member2 = new HashMap<>();
//            member2.put("@odata.type", "#microsoft.graph.user");
//            member2.put("id", "49320844-be99-4164-8167-87ff5d047ace");
//            membersDeltaList.add(member1);
//            membersDeltaList.add(member2);
            retGroupList.add(group);
        }

        return retGroupList;
    }
    @Test
    void doesGroupExist_found() throws Exception {
        String resourceGroupID = "123";
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.get(any())).thenReturn(groupCollectionResponse);

        List<Group> groupList = getTestGrouplist(3);
        when(groupCollectionResponse.getValue()).thenReturn(groupList);

        assertTrue(azureClient.doesGroupExist(resourceGroupID));
    }
    @Test
    void doesGroupExist_notfound() throws Exception {
        String resourceGroupID = "234";
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");

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
     void confirm_addgrouptoazure_triggers_post() {

         ResourceGroup resourceGroup = ResourceGroup.builder()
                 .id("12")
                 .resourceId("123")
                 .displayName("testdisplayname")
                 .identityProviderGroupObjectId("testidpgroup")
                 .resourceName("testresourcename")
                 .resourceType("testresourcetype")
                 .resourceLimit("1000")
                 .build();

         // Mock the graphServiceClient behavior
         when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
         when(groupsRequestBuilder.post(any(Group.class))).thenReturn(new Group());
         when(configGroup.getPrefix()).thenReturn("random-prefix");
         when(configGroup.getSuffix()).thenReturn("random-postfix");

         // Call the method under test
         azureClient.addGroupToAzure(resourceGroup);

         // Verify that postAsync was called once
         verify(groupsRequestBuilder, times(1)).post(any(Group.class));
     }

     // TODO: To be reimplemented after deleteGroup function has been refactored [FKS-946]
//     @Test
//     void makeSureHTTPDeleteIsCalledWhenDeleteGroupIsCalled() throws Exception {
//         when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
//         when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
//         when(groupItemRequestBuilder.get()).thenReturn(groupCollectionResponse);
//
//         azureClient.deleteGroup("delGroupID");
//
//         verify(groupItemRequestBuilder, times(1)).delete();
//     }

    // TODO: To be reimplemented after deleteGroup function has been refactored [FKS-946]
//    @Test
//    void multiplePagesWhenDeletingSingleGroupShouldThrowError {
//        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
//        when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
//        when(groupItemRequestBuilder.get()).thenReturn(groupCollectionResponse);
//        when(groupCollectionResponse.getOdataNextLink()).thenReturn("SomeFakeURL");
//
//        azureClient.deleteGroup("delGroupID");
//
//        //verify(groupItemRequestBuilder, times(1)).delete();
//    }

     @Test
     void makeSurePatchIsCalledWhenUpdateIsCalled() {

         when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
         when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
         when(groupItemRequestBuilder.patch(any(Group.class))).thenReturn(new Group());
         when(configGroup.getPrefix()).thenReturn("random-prefix");
         when(configGroup.getSuffix()).thenReturn("random-postfix");

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

         // Call the method under test
         azureClient.updateGroup(resourceGroup);

         // Verify that patchAsync is called exactly once with any Group object
         verify(groupItemRequestBuilder, times(1)).patch(any(Group.class));
     }


    @Test
    public void makeSureMSGraphExceptionIsHandledGracefully() {

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);

        // Call the method under test
        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        azureClient.addGroupMembership(resourceGroupMembership, "resourcekey");
        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));

        verify(azureGroupMembershipProducerService, times(0)).publishAddedMembership(any(AzureGroupMembership.class));
    }
     @Test
     public void makeSureAddGroupMembershipCallsHTTPPostWhenMembershipIsCorrect() {
         when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
         when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
         when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);
         when(membersRequestBuilder.ref()).thenReturn(refRequestBuilder);

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
         verify(refRequestBuilder, times(1)).post(any(ReferenceCreate.class));
     }

    @Test
    public void makeSureAddGroupMembershipDoesntPopulateKafkaWhenMSGraphThrowsError() {
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
        when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);
        when(membersRequestBuilder.ref()).thenReturn(refRequestBuilder);

        doThrow(apiException).when(refRequestBuilder).post(any(ReferenceCreate.class));

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

        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(azureGroupMembershipProducerService, times(0)).publishAddedMembership(any(AzureGroupMembership.class));
    }

    @Test
    public void makeSureHTTP400IsHandledGraceullyWhenAddingGroupMembership () {
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
        when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);
        when(membersRequestBuilder.ref()).thenReturn(refRequestBuilder);

        when(apiException.getResponseStatusCode()).thenReturn(400);
        // NOTE: Message need to contain "object reference..."
        when(apiException.getMessage()).thenReturn("Test errormessage: object references already exist");

        doThrow(apiException).when(refRequestBuilder).post(any(ReferenceCreate.class));

        String kafkaKey = "somekey";
        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        azureClient.addGroupMembership(resourceGroupMembership, kafkaKey);

        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(azureGroupMembershipProducerService, times(1)).publishAddedMembership(any(AzureGroupMembership.class));
    }
/*
    @Mock
    private DirectoryObjectItemRequestBuilder directoryObjectWithReferenceRequestBuilder;
    @Mock
    private DirectoryObjectItemRequestBuilder directoryObjectReferenceRequestBuilder;

    @Mock
    private DirectoryObjectItemRequestBuilder directoryObjectReferenceRequest;

    @Test
    public void gracefullyHandleErroneousKafkaID () {

    }
*/
    @Test
    public void makeSureDeleteGroupMembershipCallsHTTPDelete() {

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
        when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);
        when(membersRequestBuilder.byDirectoryObjectId(anyString())).thenReturn(directoryObjectItemRequestBuilder);
        when(directoryObjectItemRequestBuilder.ref()).thenReturn(singleMemberRefRequestBuilder);

        // Creating test data
        String kafkaKey = "somekey_someotherkey";

        azureClient.deleteGroupMembership(kafkaKey);

        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(singleMemberRefRequestBuilder, times(1) ).delete();
    }

    @Test
    public void logAndSkipDeletionWhenKafkaIDIswithoutUnderscore () {

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
        when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);
        when(membersRequestBuilder.byDirectoryObjectId(anyString())).thenReturn(directoryObjectItemRequestBuilder);
        when(directoryObjectItemRequestBuilder.ref()).thenReturn(singleMemberRefRequestBuilder);


        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        String kafkaKey = "exampleWithoutUnderscore";
        azureClient.deleteGroupMembership(kafkaKey);
        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(singleMemberRefRequestBuilder, times(0) ).delete();


        kafkaKey = "exampleGroupID_exampleUserID";
        azureClient.deleteGroupMembership(kafkaKey);
        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(singleMemberRefRequestBuilder, times(1) ).delete();
    }

    @Test
    public void logAndSkipDeletionWhenKafkaIDhaveMultipleUnderscores () {
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
        when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);
        when(membersRequestBuilder.byDirectoryObjectId(anyString())).thenReturn(directoryObjectItemRequestBuilder);
        when(directoryObjectItemRequestBuilder.ref()).thenReturn(singleMemberRefRequestBuilder);

        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        String kafkaKey = "example_with_multiple_underscores";
        azureClient.deleteGroupMembership(kafkaKey);
        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(singleMemberRefRequestBuilder, times(0) ).delete();

        kafkaKey = "exampleGroupID_exampleUserID";
        azureClient.deleteGroupMembership(kafkaKey);
        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(singleMemberRefRequestBuilder, times(1) ).delete();

        kafkaKey = "exampleGroupID_exampleUserID2";
        azureClient.deleteGroupMembership(kafkaKey);
        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(singleMemberRefRequestBuilder, times(2) ).delete();
    }

    // TODO: Refactor when delta is implemented [FKS-944]

    @Test
    public void makeSureDeltaIsCalledAndReturnsGroups()
    {

        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        lenient().when(configGroup.getGrouppagingsize()).thenReturn(1);
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        DeltaGetResponse deltaGetResponseTest = new DeltaGetResponse();
        deltaGetResponseTest.setValue(getTestGrouplist(3));
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
        when(deltaRequestBuilder.get(any())).thenReturn(deltaGetResponseTest);

        azureClient.pullAllGroupsDelta();
        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(azureGroupProducerService,times(3)).publish(any());

    }

    @Test
    public void makeSurePageThroughGroupsDeltaHandlesZeroGroups() {

        when(configGroup.getSuffix()).thenReturn("-suff-");
        lenient().when(configGroup.getGrouppagingsize()).thenReturn(1);
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        DeltaGetResponse deltaGetResponseTest = new DeltaGetResponse();
        deltaGetResponseTest.setValue(getTestGrouplist(0));
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
        when(deltaRequestBuilder.get(any())).thenReturn(deltaGetResponseTest);

        azureClient.pullAllGroupsDelta();
        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(azureGroupProducerService,times(0)).publish(any());

    }

    @Test
    public void makeSurePageThroughGroupsDeltaReturnsDeltaOnLastPage() {

    }

    @Test
    public void makeSurePageThroughGroupsDeltaPagesThroughGroups() {
        // Check that if OdataNextLinks is non-zero, it loops multiple times

    }

    @Test
    public void makeSurePageThroughGroupsDeltaPagesThrowsErrorIfLastPageDoesntContainDeltaLink() {
        // Make sure deltaLink Always is present on last "iteration"
    }

    @Test
    public void makeSurePageThroughGroupsDeltaPagesThroughMembers() {

        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        lenient().when(configGroup.getGrouppagingsize()).thenReturn(1);
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        DeltaGetResponse deltaGetResponseTest = new DeltaGetResponse();
        deltaGetResponseTest.setValue(getTestGrouplist(3));
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
        when(deltaRequestBuilder.get(any())).thenReturn(deltaGetResponseTest);
        when(group.getAdditionalData()).thenReturn(map);

        //when(((Map<String, Object>) additionalDataHolder).get("members@delta")).thenReturn()
        //when(untypedArray).thenReturn((UntypedArray) additionalDataHolder);
//        Map<String, Object> data = group.getAdditionalData();
//        UntypedArray retrievedArray = (UntypedArray) data.get("members@delta");
//        Map<String, Object> additionalData = group.getAdditionalData();
//        //when(additionalData.containsKey("members@delta")).thenReturn(true);
//        when(additionalData.get("members@delta")).thenReturn(retrievedArray);

        //when(untypedNode.getValue()).thenReturn(map);
        //when(group.getAdditionalData().get("members@delta")).thenReturn(map);

//        Map<String, Object> mockValue = new HashMap<>();
//        mockValue.put("members@delta", "@odata.type");
//        when(map.get(object)).thenReturn(mockValue);
        //when(untypedArray.getValue()).thenReturn((Iterable<UntypedNode>) untypedNode);

//        UntypedArray mockMembersDeltaArray = mock(UntypedArray.class);
//
//        // Create mock UntypedObject instances
//        UntypedObject member1 = mock(UntypedObject.class);
//        UntypedObject member2 = mock(UntypedObject.class);
//        when(member1.getValue().get("@odata.type").getValue()).thenReturn("#microsoft.graph.user");
//        when(member1.getValue().get("id").getValue()).thenReturn("693acd06-2877-4339-8ade-b704261fe7a0");
//
//        when(member2.getValue().get("@odata.type").getValue()).thenReturn("#microsoft.graph.user");
//        when(member2.getValue().get("id").getValue()).thenReturn("49320844-be99-4164-8167-87ff5d047ace");
//
//        // Prepare additionalData with a mocked members@delta
//        Map<String, Object> additionalData = new HashMap<>();
//        additionalData.put("members@delta", mockMembersDeltaArray);
//

        azureClient.pullAllGroupsDelta();
        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(azureGroupMembershipProducerService,times(2)).publishAddedMembership(any());

    }

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

    // TODO: Refactor when delta is implemented [FKS-944]
//    @Test
//    public void makeSureTrownErrorIsSwallowedAndNotThrown() {
//        when(graphServiceClient.groups()).thenReturn(groupItemRequestBuilder);
//        when(groupItemRequestBuilder.buildRequest()).thenReturn(groupCollectionRequest);
//        when(groupCollectionRequest.select(anyString())).thenReturn(groupCollectionRequest);
//        //when(groupCollectionRequest.expand(anyString())).thenReturn(groupCollectionRequest);
//        //when(groupCollectionRequest.filter(anyString())).thenReturn(groupCollectionRequest);
//
//        when(groupCollectionRequest.getAsync()).thenThrow(ClientException.class);
//
//        //assertDoesNotThrow( );
//        assertDoesNotThrow(()-> {
//            azureClient.pullAllGroups();
//        });
//    }

    // TODO: Refactor when delta is implemented [FKS-944]
//    @Test
//    public void shouldHandleTimeoutException() {
//        when(graphServiceClient.groups()).thenReturn(groupItemRequestBuilder);
//        //when(groupCollectionRequestBuilder.buildRequest(any(LinkedList.class))).thenReturn(groupCollectionRequest);
//        when(groupItemRequestBuilder.buildRequest()).thenReturn(groupCollectionRequest);
//        when(groupCollectionRequest.select(anyString())).thenReturn(groupCollectionRequest);
//       // when(groupCollectionRequest.expand(anyString())).thenReturn(groupCollectionRequest);
//        //when(groupCollectionRequest.filter(anyString())).thenReturn(groupCollectionRequest);
//
//        when(groupCollectionRequest.getAsync()).thenThrow(new ClientException("Timeout", new InterruptedIOException("timeout")));
//
//        /*GroupCollectionRequestBuilder mockGroupCollectionRequestBuilder2 = Mockito.mock(GroupCollectionRequestBuilder.class);
//        GroupCollectionRequest mockGroupCollectionRequest2 = Mockito.mock(GroupCollectionRequest.class);
//        GroupCollectionPage mockCollPage2= Mockito.mock(GroupCollectionPage.class);*/
//
//        /*when(groupCollectionPage.getNextPage()).thenReturn(mockGroupCollectionRequestBuilder2);
//        when(mockGroupCollectionRequestBuilder2.buildRequest()).thenReturn(mockGroupCollectionRequest2);
//        when(mockGroupCollectionRequest2.get()).thenReturn(mockCollPage2);
//
//        azureClient.pullAllGroups();
//
//        when(groupCollectionRequest.get()).thenAnswer();
//
//        azureClient.pullAllGroups();*/
//
//        /*verify(groupCollectionPage, times(2)).getNextPage();
//        verify(mockCollPage2, times(1)).getNextPage();
//    }


    @Test
     public void publishPostedMembershipToKafkaIfPostIsSuccessful()
         {
             when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
             when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);

             when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);

             when(membersRequestBuilder.ref()).thenReturn(refRequestBuilder);

             String kafkaKey = "somekey";
             ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                     .id("testid")
                     .azureGroupRef("exampleGroupRef")
                     .azureUserRef("someUserRef")
                     .roleRef("exampleRoleRef")
                     .build();

             azureClient.addGroupMembership(resourceGroupMembership, kafkaKey);

             assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
             verify(azureGroupMembershipProducerService, times(1)).publishAddedMembership(any(AzureGroupMembership.class));
         }


    @Test
     public void skipPublishingMembershipToKafkaIfResourceRefIsBad()
     {
         ApiException apiException = new ApiException("Testerror");

         when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
         when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
         when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);
         when(membersRequestBuilder.ref()).thenReturn(refRequestBuilder);

         doThrow(apiException).when(refRequestBuilder).post(any(ReferenceCreate.class));

         String kafkaKey = "somekey";
         ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                 .id("testid")
                 .azureGroupRef("exampleGroupRef")
                 .azureUserRef("someUserRef")
                 .roleRef("exampleRoleRef")
                 .build();

         azureClient.addGroupMembership(resourceGroupMembership, kafkaKey);

         assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
         verify(azureGroupMembershipProducerService, times(0)).publishAddedMembership(any(AzureGroupMembership.class));
     }

}

