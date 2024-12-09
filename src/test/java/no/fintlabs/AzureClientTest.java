package no.fintlabs;

//import com.microsoft.graph.directoryobjects.item.DirectoryObjectItemRequestBuilder;

import com.microsoft.graph.groups.GroupsRequestBuilder;
import com.microsoft.graph.groups.delta.DeltaGetResponse;
import com.microsoft.graph.groups.delta.DeltaRequestBuilder;
import com.microsoft.graph.groups.item.GroupItemRequestBuilder;
import com.microsoft.graph.groups.item.members.MembersRequestBuilder;
import com.microsoft.graph.groups.item.members.item.DirectoryObjectItemRequestBuilder;
import com.microsoft.graph.groups.item.members.ref.RefRequestBuilder;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.RequestAdapter;
import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.serialization.AdditionalDataHolder;
import com.microsoft.kiota.serialization.UntypedArray;
import com.microsoft.kiota.serialization.UntypedNode;
import com.microsoft.kiota.serialization.UntypedObject;
import com.microsoft.kiota.serialization.UntypedString;
import no.fintlabs.azure.*;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.ResourceGroupMembership;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AzureClientTest {

    @Mock
    private GraphServiceClient graphServiceClient;

    @Mock
    private GroupCollectionResponse groupCollectionResponse;

    @Mock
    private GroupsRequestBuilder groupsRequestBuilder;

    @Mock
    private UsersRequestBuilder usersRequestBuilder;


    @Mock
    private DeltaRequestBuilder deltaRequestBuilder;

    @Mock
    private RequestAdapter requestAdapter;

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
    private ConfigUser configUser;

    @Mock
    private Config config;

    @InjectMocks
    private AzureClient azureClient;

    @Mock
    private FintCache<String, AzureUser> entraIdUserCache;

    @Mock
    private FintCache<String, Optional> resourceGroupMembershipCache;

    @Mock
    private AzureUserProducerService azureUserProducerService;

    @Mock
    private AzureUserExternalProducerService azureUserExternalProducerService;


    @Mock
    MembersRequestBuilder membersRequestBuilder;

    @Mock
    ApiException apiException;

    @Mock
    RefRequestBuilder refRequestBuilder;

    @Mock
    com.microsoft.graph.groups.item.members.item.ref.RefRequestBuilder singleMemberRefRequestBuilder;


    @BeforeEach
    public void reset() {
        Mockito.reset(
                graphServiceClient,
                groupCollectionResponse,
                groupsRequestBuilder,
                usersRequestBuilder,
                deltaRequestBuilder,
                requestAdapter,
                groupItemRequestBuilder,
                azureGroupProducerService,
                azureGroupMembershipProducerService,
                directoryObjectItemRequestBuilder,
                configGroup,
                configUser,
                config,
                entraIdUserCache,
                resourceGroupMembershipCache,
                azureUserProducerService,
                azureUserExternalProducerService
        );
    }

    private UntypedObject getTestUser(boolean removed) {
        Map<String, UntypedNode> userMap = new HashMap<>();
        userMap.put("@odata.type", new UntypedString("#microsoft.graph.user"));
        userMap.put("id", new UntypedString(UUID.randomUUID().toString()));

        if (removed) {
            userMap.put("@removed", new UntypedObject(
                    Map.of(
                            "reason", new UntypedString("deleted")
                    )
            ));
        }
        return new UntypedObject(userMap);
    }

    private UntypedArray getDeltaMembers(int numUsersAdded, int numUsersRemoved) {
        List<UntypedNode> users = new ArrayList<>();
        for (int i = 0; i < numUsersAdded; i++) {
            users.add(getTestUser(false));
        }
        for (int i = 0; i < numUsersRemoved; i++) {
            users.add(getTestUser(true));
        }
        return new UntypedArray(users);
    }

    private List<Group> getTestGrouplistAddedRemoved(int numberOfGroups, int nUsersAdded, int nUsersRemoved) {
        List<Group> retGroupList = new ArrayList<>();
        for (int i=0; i<numberOfGroups; i++) {
            //String randomIntInRange = String.valueOf(new Random().nextInt(100) + 1);
            Group group = new Group();
            group.setId(String.valueOf(i));
            group.setDisplayName("testgroup" + i + "-suff-");
            HashMap<String, Object> additionalData = new HashMap<>() {{
                put("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId", group.getId());
                put("members@delta", getDeltaMembers(nUsersAdded, nUsersRemoved));
            }};
            group.setAdditionalData(additionalData);
            retGroupList.add(group);
        }

        return retGroupList;
    }

    private List<Group> getTestGrouplist(int numberOfGroups, int numberOfUsers) {
        return getTestGrouplistAddedRemoved(numberOfGroups, numberOfUsers, 0);
        /*List<Group> retGroupList = new ArrayList<>();
        for (int i=0; i<numberOfGroups; i++) {
            Group group = new Group();
            group.setId(UUID.randomUUID().toString());
            group.setDisplayName("testgroup" + i + "-suff-");
            HashMap<String, Object> additionalData = new HashMap<>() {{
                put("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId", "123");
                put("members@delta", getDeltaMembers(numberOfUsers));
            }};
            group.setAdditionalData(additionalData);
            retGroupList.add(group);
        }

        return retGroupList;*/
    }

    @Test
    void doesGroupExist_found() throws Exception {
        List<Group> groupList = getTestGrouplist(1, 1);
        when(groupCollectionResponse.getValue()).thenReturn(groupList);
        String resourceGroupID = groupList.get(0).getId();
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.get(any())).thenReturn(groupCollectionResponse);
        assertTrue(azureClient.doesGroupExist(resourceGroupID));
    }

    @Test
    void doesGroupExist_notfound() throws Exception {
        String resourceGroupID = "234";
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.get(any())).thenReturn(groupCollectionResponse);

        List<Group> groupList = getTestGrouplist(3, 3);
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
    void confirm_addgrouptoazure_contains_FintKontrollRoleId_Attribute() {

        ResourceGroup resourceGroup = ResourceGroup.builder()
                .id("12")
                .resourceId("123")
                .displayName("testdisplayname")
                .identityProviderGroupObjectId("testidpgroup")
                .resourceName("testresourcename")
                .resourceType("testresourcetype")
                .resourceLimit("1000")
                .build();

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.post(any(Group.class))).thenReturn(new Group());
        when(configGroup.getPrefix()).thenReturn("random-prefix");
        when(configGroup.getSuffix()).thenReturn("random-postfix");
        when(config.getEntobjectid()).thenReturn("testentobjectid123");
        when(configGroup.getFintkontrollidattribute()).thenReturn("RoleKontrollIdAttribute");
        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);

        // Act
        azureClient.addGroupToAzure(resourceGroup);

        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));

        // Assert
        verify(groupsRequestBuilder, times(1)).post(groupCaptor.capture());

        Group capturedGroup = groupCaptor.getValue();
        assertNotNull(capturedGroup);
        assertNotNull(capturedGroup.getAdditionalData());
        assertTrue(capturedGroup.getAdditionalData().containsKey("RoleKontrollIdAttribute"));
        assertEquals("12", capturedGroup.getAdditionalData().get("RoleKontrollIdAttribute"));

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

         when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
         when(groupsRequestBuilder.post(any(Group.class))).thenReturn(new Group());
         when(configGroup.getPrefix()).thenReturn("random-prefix");
         when(configGroup.getSuffix()).thenReturn("random-postfix");
         when(config.getEntobjectid()).thenReturn("testentobjectid123");

         azureClient.addGroupToAzure(resourceGroup);

         assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
         verify(groupsRequestBuilder, times(1)).post(any(Group.class));
     }

    @Test
    void confirm_addgrouptoazure_fails_if_resourceGroup_IsMissing_Attributes() {

        ResourceGroup resourceGroup = ResourceGroup.builder()
                .id("1254")
                .resourceId("12354")
                //.displayName("testdisplayname")
                .identityProviderGroupObjectId("testidpgroup32")
                .resourceName(null)
                .resourceType(null)
                .resourceLimit("1000")
                .build();

//        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
//        when(groupsRequestBuilder.post(any(Group.class))).thenReturn(new Group());
//        when(configGroup.getPrefix()).thenReturn("random-prefix");
//        when(configGroup.getSuffix()).thenReturn("random-postfix");
//        when(config.getEntobjectid()).thenReturn("testentobjectid123");

        azureClient.addGroupToAzure(resourceGroup);

        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(groupsRequestBuilder, times(0)).post(any(Group.class));
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

         azureClient.updateGroup(resourceGroup);

         verify(groupItemRequestBuilder, times(1)).patch(any(Group.class));
     }


    @Test
    void makeSureMSGraphExceptionIsHandledGracefully() {

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
     void makeSureAddGroupMembershipCallsHTTPPostWhenMembershipIsCorrect() {
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
         verify(refRequestBuilder, times(1)).post(any(ReferenceCreate.class));
     }

    @Test
    void makeSureAddGroupMembershipDoesntPopulateKafkaWhenMSGraphThrowsError() {
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

    @Test
    void makeSureHTTP400IsHandledGraceullyWhenAddingGroupMembership () {
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
    void makeSureDeleteGroupMembershipCallsHTTPDelete() {

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
    void logAndSkipDeletionWhenKafkaIDIswithoutUnderscore () {

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
        when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);
        when(membersRequestBuilder.byDirectoryObjectId(anyString())).thenReturn(directoryObjectItemRequestBuilder);
        when(directoryObjectItemRequestBuilder.ref()).thenReturn(singleMemberRefRequestBuilder);


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
    void logAndSkipDeletionWhenKafkaIDhaveMultipleUnderscores () {
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

    // TODO: What are we actually testing here? Nothing is returned.
    // 3 random groups with 3 randoms produces 3 groups
    @Test
    void makeSureDeltaIsCalledWhenGroupsAreDefinedAndPublishesCorrectNumberOfGroupsAndMembershipsToKafka()
    {
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        lenient().when(configGroup.getGrouppagingsize()).thenReturn(1);
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);

        DeltaGetResponse deltaGetResponseTest = new DeltaGetResponse();
        deltaGetResponseTest.setValue(getTestGrouplist(3,3 ));
        when(deltaRequestBuilder.get(any())).thenReturn(deltaGetResponseTest);

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> azureClient.pullAllGroupsDelta()).join();
        assertTrue(testPool.awaitQuiescence(15, TimeUnit.SECONDS));


        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(azureGroupProducerService,times(3)).publish(any());
        // Verify correct number of publish is called for membership
        verify(azureGroupMembershipProducerService, times(9)).publishAddedMembership(any());
    }


    @Test
    void makeSure18NewUsersAreCreatedAnd9AreRemoved_And_removed_From_Cache() {
        // Mock setup
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);

        DeltaGetResponse deltaGetResponseTest = new DeltaGetResponse();
        deltaGetResponseTest.setValue(getTestGrouplistAddedRemoved(3, 6, 3));
        deltaGetResponseTest.setOdataDeltaLink("delta link");

        when(deltaRequestBuilder.get(any())).thenReturn(deltaGetResponseTest);

        // Thread pool for isolation
        ForkJoinPool testPool = new ForkJoinPool(4);
        testPool.submit(() -> azureClient.pullAllGroupsDelta()).join();
        assertTrue(testPool.awaitQuiescence(15, TimeUnit.SECONDS), "Not all tasks completed in time");

        verify(azureGroupProducerService, times(3)).publish(any(AzureGroup.class));
        verify(azureGroupMembershipProducerService, times(18)).publishAddedMembership(any(AzureGroupMembership.class));
        verify(azureGroupMembershipProducerService, times(9)).publishDeletedMembership(anyString());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(resourceGroupMembershipCache, times(9)).remove(captor.capture());
    }


    @Test
    void makeSurePageThroughGroupsDeltaHandlesZeroGroups() {

        when(configGroup.getSuffix()).thenReturn("-suff-");
        lenient().when(configGroup.getGrouppagingsize()).thenReturn(1);
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");

        DeltaGetResponse deltaGetResponseTest = new DeltaGetResponse();
        deltaGetResponseTest.setValue(getTestGrouplist(0,0));
        deltaGetResponseTest.setOdataDeltaLink("delta link");

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);

        when(deltaRequestBuilder.get(any())).thenReturn(deltaGetResponseTest);

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> azureClient.pullAllGroupsDelta()).join();
        assertTrue(testPool.awaitQuiescence(15, TimeUnit.SECONDS));
        verify(azureGroupProducerService,times(0)).publish(any());
    }

    @Test
    void makeSureDeltaFunctionFailsIfODataDeltaLinkIsUndefinedOnLastPage() {

        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);

        DeltaGetResponse firstPage = new DeltaGetResponse();
        firstPage.setValue(getTestGrouplistAddedRemoved(3, 6, 3));
        firstPage.setOdataNextLink("LinkToSecondPage");

        DeltaGetResponse secondPage = new DeltaGetResponse();
        secondPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        secondPage.setOdataNextLink("LinkToThirdPage");

        DeltaGetResponse lastPage = new DeltaGetResponse();
        lastPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        lastPage.setOdataNextLink(null);
        lastPage.setOdataDeltaLink(null);

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
        when(deltaRequestBuilder.get(any())).thenReturn(firstPage);
        when(requestAdapter.send(any(RequestInformation.class), any(), any()))
                .thenReturn(secondPage, lastPage);

        when(deltaRequestBuilder.withUrl("LinkToSecondPage")).thenReturn(deltaRequestBuilder);


        assertThrows(NullPointerException.class,
                () -> azureClient.pullAllGroupsDelta(),
                "Expected pullAllGroupsDelta to throw a NullPointerException when the delta link is missing on the last page."
        );
        assertNull(lastPage.getOdataDeltaLink(), "Last page should not have a delta link.");
    }

    @Test
    void makeSurePageThroughGroupsDeltaReturnsDeltaOnLastPage() {
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);

        DeltaGetResponse firstPage = new DeltaGetResponse();
        firstPage.setValue(getTestGrouplistAddedRemoved(3, 6, 3));
        firstPage.setOdataNextLink("LinkToSecondPage");

        DeltaGetResponse secondPage = new DeltaGetResponse();
        secondPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        secondPage.setOdataNextLink("LinkToThirdPage");

        DeltaGetResponse thirdPage = new DeltaGetResponse();
        thirdPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        thirdPage.setOdataDeltaLink("delta link");

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
        when(deltaRequestBuilder.get(any())).thenReturn(firstPage);

        when(requestAdapter.send(any(RequestInformation.class), any(), any()))
                .thenReturn(secondPage, thirdPage);

        DeltaGetResponse lastPage = new DeltaGetResponse();
        lastPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        lastPage.setOdataNextLink(null);
        lastPage.setOdataDeltaLink("last delta link");

        when(deltaRequestBuilder.get()).thenReturn(lastPage);
        when(deltaRequestBuilder.withUrl("LinkToSecondPage")).thenReturn(deltaRequestBuilder);

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> azureClient.pullAllGroupsDelta()).join();
        assertTrue(testPool.awaitQuiescence(15, TimeUnit.SECONDS));

        verify(azureGroupProducerService, times(4)).publish(any());
        verify(requestAdapter, times(2)).send(any(RequestInformation.class), any(), any());
        verify(groupsRequestBuilder, times(2)).delta();
        verify(graphServiceClient, times(2)).groups();
        verify(deltaRequestBuilder, times(1)).withUrl("LinkToSecondPage");
        verify(deltaRequestBuilder, times(1)).get(any());
        verify(deltaRequestBuilder, times(1)).get();

        assertNull(lastPage.getOdataNextLink(), "Last page should not have a next link.");
        assertNotNull(lastPage.getOdataDeltaLink(), "Last page should have a delta link.");
        assertEquals("last delta link", lastPage.getOdataDeltaLink(), "Delta link should match expected value.");

    }

    @Test
    void makeSurePageThroughGroupsDeltaPagesThroughPages() {
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);

        DeltaGetResponse firstPage = new DeltaGetResponse();
        firstPage.setValue(getTestGrouplistAddedRemoved(3, 6, 3));
        firstPage.setOdataNextLink("LinkToSecondPage");

        DeltaGetResponse secondPage = new DeltaGetResponse();
        secondPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        secondPage.setOdataNextLink("LinkToThirdPage");

        DeltaGetResponse thirdPage = new DeltaGetResponse();
        thirdPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        thirdPage.setOdataDeltaLink("delta link");

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
        when(deltaRequestBuilder.get(any())).thenReturn(firstPage);

        when(requestAdapter.send(any(RequestInformation.class), any(), any()))
                .thenReturn(secondPage, thirdPage);

        DeltaGetResponse lastPage = new DeltaGetResponse();
        lastPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        lastPage.setOdataNextLink(null);
        lastPage.setOdataDeltaLink("last delta link");

        when(deltaRequestBuilder.get()).thenReturn(lastPage);
        when(deltaRequestBuilder.withUrl("LinkToSecondPage")).thenReturn(deltaRequestBuilder);

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> azureClient.pullAllGroupsDelta()).join();
        assertTrue(testPool.awaitQuiescence(15, TimeUnit.SECONDS));

        verify(azureGroupProducerService, times(4)).publish(any());
        verify(requestAdapter, times(2)).send(any(RequestInformation.class), any(), any());
        verify(groupsRequestBuilder, times(2)).delta();
        verify(graphServiceClient, times(2)).groups();
        verify(deltaRequestBuilder, times(1)).withUrl("LinkToSecondPage");
        verify(deltaRequestBuilder, times(1)).get(any());
        verify(deltaRequestBuilder, times(1)).get();
    }

    @Test
    void makeSureUserIsnotRepublishedIfUserCacheContainsUserAndExternalUserIsPublished()
    {
        when(configUser.getExternaluserattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute11");
        when(configUser.getExternaluservalue()).thenReturn("novari");
        when(configUser.getEmployeeidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute10");
        when(configUser.getStudentidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute9");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.users()).thenReturn(usersRequestBuilder);

        OnPremisesExtensionAttributes onPremAttributes = new OnPremisesExtensionAttributes();
        onPremAttributes.setExtensionAttribute10("1236");
        User user = new User();
        user.setId("1234");
        user.setMail("testuser1@mail.com");
        user.setUserPrincipalName("testuser1@mail.com");
        user.setAccountEnabled(true);
        user.setOnPremisesExtensionAttributes(onPremAttributes);

        OnPremisesExtensionAttributes onPremAttributes2 = new OnPremisesExtensionAttributes();
        onPremAttributes2.setExtensionAttribute10("4566");
        User user2 = new User();
        user2.setId("4565");
        user2.setMail("testuser2@mail.com");
        user2.setUserPrincipalName("testuser2@mail.com");
        user2.setAccountEnabled(true);
        user2.setOnPremisesExtensionAttributes(onPremAttributes2);


        User extUser = new User();
        extUser.setId("7896");
        extUser.setMail("testExtuser2@mail.com");
        extUser.setUserPrincipalName("testExtuser2@mail.com");
        extUser.setAccountEnabled(true);
        OnPremisesExtensionAttributes onPremAttributes3 = new OnPremisesExtensionAttributes();
        onPremAttributes3.setExtensionAttribute11("novari");
        extUser.setOnPremisesExtensionAttributes(onPremAttributes3);

        List<User> userList = new ArrayList<>();
        userList.add(user);
        userList.add(user2);
        userList.add(extUser);

        UserCollectionResponse firstPage = new UserCollectionResponse();
        firstPage.setValue(userList);
        when(usersRequestBuilder.get(any())).thenReturn(firstPage);

        AzureUser cachedUser = new AzureUser(user, configUser);
        AzureUser nonCachedUser = new AzureUser(user2, configUser);
        lenient().when(entraIdUserCache.containsKey(user.getId())).thenReturn(true);
        lenient().when(entraIdUserCache.get(user.getId())).thenReturn(cachedUser);

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> azureClient.pullAllUsers()).join();
        assertTrue(testPool.awaitQuiescence(15, TimeUnit.SECONDS));

        verify(azureUserProducerService, times(1)).publish(nonCachedUser);
        verify(azureUserExternalProducerService, times(1)).publish(any(AzureUserExternal.class));


    }

    @Test
    void makeSureAzureUserIsNotPublishedIfAzureUserGetAttributeValueIsNull()
    {
        when(configUser.getExternaluserattribute()).thenReturn("state");
        when(configUser.getEmployeeidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute10");
        when(configUser.getStudentidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute9");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.users()).thenReturn(usersRequestBuilder);


        OnPremisesExtensionAttributes onPremAttributes = new OnPremisesExtensionAttributes();
        onPremAttributes.setExtensionAttribute10("123");
        User user = new User();
        user.setId("123");
        user.setMail("testuser1@mail.com");
        user.setUserPrincipalName("testuser1@mail.com");
        user.setAccountEnabled(true);
        user.setOnPremisesExtensionAttributes(onPremAttributes);

        //OnPremisesExtensionAttributes onPremAttributes2 = new OnPremisesExtensionAttributes();
        //onPremAttributes2.setExtensionAttribute10("456");
        User user2 = new User();
        user2.setId("456");
        user2.setMail("testuser2@mail.com");
        user2.setUserPrincipalName("testuser2@mail.com");
        user2.setAccountEnabled(true);
        //user2.setOnPremisesExtensionAttributes(onPremAttributes2);

        List<User> userList = new ArrayList<>();
        userList.add(user);
        userList.add(user2);

        UserCollectionResponse firstPage = new UserCollectionResponse();
        firstPage.setValue(userList);
        when(usersRequestBuilder.get(any())).thenReturn(firstPage);

        AzureUser cachedUser = new AzureUser(user, configUser);
        AzureUser notCachedUser = new AzureUser(user2, configUser);
        lenient().when(entraIdUserCache.containsKey(user.getId())).thenReturn(true);
        lenient().when(entraIdUserCache.get(user.getId())).thenReturn(cachedUser);

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> azureClient.pullAllUsers()).join();
        assertTrue(testPool.awaitQuiescence(15, TimeUnit.SECONDS));


        verify(azureUserProducerService, times(0)).publish(notCachedUser);
    }

    @Test
    void makeSurePageThroughGroupsDeltaPagesThroughMembers() {

        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        lenient().when(configGroup.getGrouppagingsize()).thenReturn(1);
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        DeltaGetResponse deltaGetResponseTest = new DeltaGetResponse();

        // 3 groups with exactly 3 members in each group.
        deltaGetResponseTest.setValue(getTestGrouplist(3, 3));
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);

        when(deltaRequestBuilder.get(any())).thenReturn(deltaGetResponseTest);

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> azureClient.pullAllGroupsDelta()).join();
        assertTrue(testPool.awaitQuiescence(15, TimeUnit.SECONDS));


        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS));
        verify(azureGroupMembershipProducerService,times(9)).publishAddedMembership(any(AzureGroupMembership.class));

    }

    @Test
    void makeSureUserIsnotRepublishedIfUserCacheContainsUser()
    {
        when(configUser.getExternaluserattribute()).thenReturn("state");
        //when(configUser.getExternaluservalue()).thenReturn("frid");
        when(configUser.getEmployeeidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute10");
        when(configUser.getStudentidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute9");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.users()).thenReturn(usersRequestBuilder);


        OnPremisesExtensionAttributes onPremAttributes = new OnPremisesExtensionAttributes();
        onPremAttributes.setExtensionAttribute10("123");
        User user = new User();
        user.setId("123");
        user.setMail("testuser1@mail.com");
        user.setUserPrincipalName("testuser1@mail.com");
        user.setAccountEnabled(true);
        user.setOnPremisesExtensionAttributes(onPremAttributes);

        OnPremisesExtensionAttributes onPremAttributes2 = new OnPremisesExtensionAttributes();
        onPremAttributes2.setExtensionAttribute10("456");
        User user2 = new User();
        user2.setId("456");
        user2.setMail("testuser2@mail.com");
        user2.setUserPrincipalName("testuser2@mail.com");
        user2.setAccountEnabled(true);
        user2.setOnPremisesExtensionAttributes(onPremAttributes2);

        List<User> userList = new ArrayList<>();
        userList.add(user);
        userList.add(user2);

        UserCollectionResponse firstPage = new UserCollectionResponse();
        firstPage.setValue(userList);
        when(usersRequestBuilder.get(any())).thenReturn(firstPage);

        AzureUser cachedUser = new AzureUser(user, configUser);
        AzureUser notCachedUser = new AzureUser(user2, configUser);
        lenient().when(entraIdUserCache.containsKey(user.getId())).thenReturn(true);
        lenient().when(entraIdUserCache.get(user.getId())).thenReturn(cachedUser);

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> azureClient.pullAllUsers()).join();
        assertTrue(testPool.awaitQuiescence(15, TimeUnit.SECONDS));
        verify(azureUserProducerService, times(1)).publish(notCachedUser);

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
    void publishPostedMembershipToKafkaIfPostIsSuccessful()
         {
             when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
             when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);

             when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);

             when(membersRequestBuilder.ref()).thenReturn(refRequestBuilder);

             String kafkaKey = "somekey1";
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
     void skipPublishingMembershipToKafkaIfResourceRefIsBad()
     {
         ApiException apiException = new ApiException("Testerror");

         when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
         when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
         when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);
         when(membersRequestBuilder.ref()).thenReturn(refRequestBuilder);

         doThrow(apiException).when(refRequestBuilder).post(any(ReferenceCreate.class));

         String kafkaKey = "somekey2";
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

