package no.fintlabs;

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
import com.microsoft.kiota.serialization.UntypedArray;
import com.microsoft.kiota.serialization.UntypedNode;
import com.microsoft.kiota.serialization.UntypedObject;
import com.microsoft.kiota.serialization.UntypedString;
import no.fintlabs.azure.*;
import no.fintlabs.config.Config;
import no.fintlabs.config.ConfigGroup;
import no.fintlabs.config.ConfigUser;
import no.fintlabs.db.*;
import no.fintlabs.group.MsGraphGroup;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.ResourceGroupMembership;
import no.fintlabs.user.MsGraphUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;


@Disabled
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

    @Mock
    private Config.Credentials configcredentials;
    @Spy
    DBObjectListOrchestrator orchestrator;
    @InjectMocks
    private MsGraphGroup msGraphGroup;

    @InjectMocks
    private MsGraphUser msGraphUser;


    //@Mock
    //private ConcurrentHashMap<String, AzureUser> entraIdUserCache;


    @Mock
    DBObjectList<DBUser> orchestratoruserlist;

    @Mock
    DBObjectList<DBMembership> orchestratormemberships;

    @Mock
    DBObjectList<DBGroup> orchestratorgrouplist;

    @Mock
    private HashSet<String> azureGroupMembershipCache;

    @Mock
    private ResourceGroupMembership resourceGroupMembership;

    /*@Spy
    private ConcurrentHashMap<String, Optional<ResourceGroupMembership>> resourceGroupMembershipCache =
            new ConcurrentHashMap<>();**/

    /*@Spy
    private Set<String> membershipCache = ConcurrentHashMap.newKeySet();*/

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

    @Mock
    com.microsoft.graph.groups.getbyids.GetByIdsRequestBuilder getByIdsRequestBuilder;

    @AfterEach
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
                orchestrator,
                azureUserProducerService,
                azureUserExternalProducerService,
                membersRequestBuilder,
                apiException,
                refRequestBuilder
        );
        orchestrator.clear();
    }

    public TestUtils.TestGroupData toTestGroupData(List<Group> groups) {
        List<String> createdKeys = new ArrayList<>();
        List<String> removedKeys = new ArrayList<>();

        for (Group g : groups) {
            String groupId = g.getId();
            UntypedArray members = (UntypedArray) g.getAdditionalData().get("members@delta");

            for (UntypedNode n : members.getValue()) {
                UntypedObject uo = (UntypedObject) n;
                String userId = ((UntypedString) uo.getValue().get("id")).getValue();
                String key = groupId + "_" + userId;

                if (uo.getValue().containsKey("@removed")) {
                    removedKeys.add(key);
                } else {
                    createdKeys.add(key);
                }
            }
        }
        return new TestUtils.TestGroupData(groups, removedKeys, createdKeys);
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
    }

    @Test
    void doesGroupExist_found() throws Exception {
        // TEST OK
        List<Group> groupList = getTestGrouplist(1, 1);
        when(groupCollectionResponse.getValue()).thenReturn(groupList);

        String resourceGroupID = groupList.getFirst().getId();
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.get(any())).thenReturn(groupCollectionResponse);

        assertTrue(msGraphGroup.doesGroupExist(resourceGroupID));
    }

    @Test
    void doesGroupExist_notfound() throws Exception {
        // TEST OK
        String resourceGroupID = "234";
        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.get(any())).thenReturn(groupCollectionResponse);

        List<Group> groupList = getTestGrouplist(3, 3);
        when(groupCollectionResponse.getValue()).thenReturn(groupList);

        assertFalse(msGraphGroup.doesGroupExist(resourceGroupID));
    }

    @Test
    void doesGroupExist_throwswhennextpageisindicated() {
        // TEST OK
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.get(any())).thenReturn(groupCollectionResponse);
        when(groupCollectionResponse.getOdataNextLink()).thenReturn("somefakeurl");

        assertThrows(Exception.class,
                () -> msGraphGroup.doesGroupExist("123")
        );
    }

    @Test
    void confirm_addgrouptoazure_contains_FintKontrollRoleId_Attribute() {
        // TEST OK
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
        when(config.getCredentials()).thenReturn(configcredentials);
        when(configcredentials.getEntobjectid()).thenReturn("testentobjectid123");
        when(configGroup.getFintkontrollidattribute()).thenReturn("RoleKontrollIdAttribute");
        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);

        msGraphGroup.addGroupToAzureAsync(resourceGroup);

        //assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, SECONDS));

        // Assert
        await().atMost(5, SECONDS).untilAsserted(() -> {
                    verify(groupsRequestBuilder, times(1)).post(groupCaptor.capture());
                });

        Group capturedGroup = groupCaptor.getValue();

        assertNotNull(capturedGroup);
        assertNotNull(capturedGroup.getAdditionalData());
        assertTrue(capturedGroup.getAdditionalData().containsKey("RoleKontrollIdAttribute"));
        assertEquals("12", capturedGroup.getAdditionalData().get("RoleKontrollIdAttribute"));
    }

    @Test
    void confirm_addgrouptoazure_triggers_post() {
        // TEST OK
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
        when(config.getCredentials()).thenReturn(configcredentials);
        when(configcredentials.getEntobjectid()).thenReturn("testentobjectid123");

        msGraphGroup.addGroupToAzureAsync(resourceGroup);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(groupsRequestBuilder, times(1)).post(any(Group.class));
        });
    }

    @Test
    void confirm_addgrouptoazure_fails_if_resourceGroup_IsMissing_Attributes() {
        // TEST OK
        ResourceGroup resourceGroup = ResourceGroup.builder()
                .id("1254")
                .resourceId("12354")
                //.displayName("testdisplayname")
                .identityProviderGroupObjectId("testidpgroup32")
                .resourceName(null)
                .resourceType(null)
                .resourceLimit("1000")
                .build();

        msGraphGroup.addGroupToAzureAsync(resourceGroup);

        //assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, SECONDS));
        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(groupsRequestBuilder, times(0)).post(any(Group.class));
        });
    }

    @Test
    void makeSurePatchIsCalledWhenUpdateIsCalled() {
        // TEST OK
         when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
         when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
         when(groupItemRequestBuilder.patch(any(Group.class))).thenReturn(new Group());
         when(configGroup.getPrefix()).thenReturn("random-prefix");
         when(configGroup.getSuffix()).thenReturn("random-postfix");

         ResourceGroup resourceGroup = ResourceGroup.builder()
                 .id("12")
                 .resourceId("123")
                 .displayName("testdisplayname")
                 .identityProviderGroupObjectId("testidpgroup")
                 .resourceName("testresourcename")
                 .resourceType("testresourcetype")
                 .resourceLimit("1000")
                 .build();

        ForkJoinPool testPool = new ForkJoinPool();
        testPool.submit(() -> msGraphGroup.updateGroup(resourceGroup)).join();

        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(groupItemRequestBuilder, times(1)).patch(any(Group.class));
        });
     }

    @Test
    void makeSureMSGraphExceptionIsHandledGracefully() {
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);

        ApiException api = mock(ApiException.class);
        doThrow(api).when(refRequestBuilder).post(any(ReferenceCreate.class));

        ResourceGroupMembership m = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        msGraphGroup.addGroupMembership(m, "resourcekey");

        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(azureGroupMembershipProducerService, never()).addMembership(any(AzureGroupMembership.class));
            verify(azureGroupMembershipProducerService, never()).removeMembership(any(AzureGroupMembership.class));
        });
    }

    @Test
    void makeSureAddGroupMembershipCallsHTTPPostWhenMembershipIsCorrect() {
        // TEST OK
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

        msGraphGroup.addGroupMembership(resourceGroupMembership, kafkaKey);

        await().atMost(5, SECONDS).untilAsserted(() ->
                verify(refRequestBuilder, times(1)).post(any(ReferenceCreate.class))
        );
     }

    @Test
    void makeSureAddGroupMembershipDoesntPopulateKafkaWhenMSGraphThrowsError() {
        // TEST OK
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

        msGraphGroup.addGroupMembership(resourceGroupMembership, kafkaKey);

        //assertTrue(ForkJoinPool.commonPool().awaitQuiescence(15, SECONDS));
        await().atMost(5, SECONDS).untilAsserted(() -> {
                verify(azureGroupMembershipProducerService, times(0)).addMembership(any(AzureGroupMembership.class));
                });
        //verify(azureGroupMembershipProducerService, times(0)).addMembership(any(AzureGroupMembership.class));
    }

    @Test
    void makeSureHTTP400IsHandledGraceullyWhenAddingGroupMembership () {
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
        when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);
        when(membersRequestBuilder.ref()).thenReturn(refRequestBuilder);

        when(apiException.getResponseStatusCode()).thenReturn(400);
        when(apiException.getMessage()).thenReturn("Test errormessage: object references already exist");

        doThrow(apiException).when(refRequestBuilder).post(any(ReferenceCreate.class));

        String kafkaKey = "somekey";
        ResourceGroupMembership resourceGroupMembership = ResourceGroupMembership.builder()
                .id("testid")
                .azureGroupRef("exampleGroupRef")
                .azureUserRef("someUserRef")
                .roleRef("exampleRoleRef")
                .build();

        msGraphGroup.addGroupMembership(resourceGroupMembership, kafkaKey);

        //assertTrue(ForkJoinPool.commonPool().awaitQuiescence(15, SECONDS));

        // TODO: Needs refactoring
        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(azureGroupMembershipProducerService, times(1)).addMembership(any(AzureGroupMembership.class));
        });
    }

    @Test
    void makeSureDeleteGroupMembershipCallsHTTPDelete() {

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
        when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);
        when(membersRequestBuilder.byDirectoryObjectId(anyString())).thenReturn(directoryObjectItemRequestBuilder);
        when(directoryObjectItemRequestBuilder.ref()).thenReturn(singleMemberRefRequestBuilder);

        // Creating test data
        String kafkaKey = "somekey_someotherkey";

        msGraphGroup.deleteGroupMembership(kafkaKey);

//        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, SECONDS));
//        // TODO: Needs refactoring
        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(singleMemberRefRequestBuilder, times(1)).delete();
        });
    }

    @Test
    void logAndSkipDeletionWhenKafkaIDIswithoutUnderscore () {
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
        when(groupItemRequestBuilder.members()).thenReturn(membersRequestBuilder);
        when(membersRequestBuilder.byDirectoryObjectId(anyString())).thenReturn(directoryObjectItemRequestBuilder);
        when(directoryObjectItemRequestBuilder.ref()).thenReturn(singleMemberRefRequestBuilder);

        msGraphGroup.deleteGroupMembership("exampleWithoutUnderscore");
        verifyNoInteractions(singleMemberRefRequestBuilder);

//        // 2) Valid key → must call delete()
//        azureClient.deleteGroupMembership("exampleGroupID_exampleUserID");
//
//        // If your SDK's delete takes a config arg, use delete(any()) instead of delete()
//        verify(singleMemberRefRequestBuilder, times(1)).delete();
    }

    @Test
    void logAndSkipDeletionWhenKafkaIDhaveMultipleUnderscores () {
        // TODO: Needs refactoring
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
        msGraphGroup.deleteGroupMembership(kafkaKey);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            verifyNoInteractions(singleMemberRefRequestBuilder);
//        verify(singleMemberRefRequestBuilder, times(0) ).delete();
//
//        kafkaKey = "exampleGroupID_exampleUserID";
//        azureClient.deleteGroupMembership(kafkaKey);
//        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, SECONDS));
//        verify(singleMemberRefRequestBuilder, times(1) ).delete();
//
//        kafkaKey = "exampleGroupID_exampleUserID2";
//        azureClient.deleteGroupMembership(kafkaKey);
//        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, SECONDS));
//        verify(singleMemberRefRequestBuilder, times(2) ).delete();
        });
    }

    // 3 random groups with 3 randoms produces 3 groups, and 9 posts to kafka
    @Test
    void makeSureDeltaIsCalledWhenGroupsAreDefinedAndPublishesCorrectNumberOfGroupsAndMembershipsToKafka() {
        // TEST OK
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
        testPool.submit(() -> msGraphGroup.pullAllGroupsDelta()).join();
        assertTrue(testPool.awaitQuiescence(15, SECONDS));

        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(azureGroupProducerService, times(3)).processGroup(any());
            verify(azureGroupMembershipProducerService, times(9)).addMembership(any());
        });
    }

    @Test
    void makeSure18NewUsersArePublishedOnKafkaAnd9RemovedUsersAreIgnoredSinceTheyAreNotInCache() {

        // Override the constructor for cache
        ReflectionTestUtils.setField(msGraphGroup, "orchestrator", orchestrator);

        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute())
                .thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);

        DeltaGetResponse delta = new DeltaGetResponse();
        var testGroups = getTestGrouplistAddedRemoved(3, 6, 3);
        delta.setValue(testGroups);
        TestUtils.TestGroupData testGroupData = toTestGroupData(testGroups);
        delta.setOdataDeltaLink("delta link");

        when(deltaRequestBuilder.get(any())).thenReturn(delta);

        msGraphGroup.pullAllGroupsDelta();
        DBObjectList<DBMembership> memberships = spy(DBObjectList.class);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(azureGroupProducerService, times(3)).processGroup(any(AzureGroup.class));
            verify(azureGroupMembershipProducerService, times(18)).addMembership(any(AzureGroupMembership.class));
            verify(memberships, times(9)).remove(anyString());
            verify(azureGroupMembershipProducerService, never()).removeMembership(any(AzureGroupMembership.class));
        });

    }

    @Test
    void makeSure18NewUsersArePublishedOnKafkaAnd9ArePublishedAsRemovedOnKafkaAnd9IsremovedFromCache() {

        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute())
                .thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
        when(orchestrator.getMemberships()).thenReturn(orchestratormemberships);

        TestUtils.DBObjectListOrchestratorTest testdata = new TestUtils.DBObjectListOrchestratorTest();
        testdata.generateNRandomUsers(50);
        // TODO: Should fail harder if generation fails
        testdata.generateNRandomGroupsWithNMemberships(5,2,6);

        when(orchestrator.getUsers()).thenReturn(testdata.getUsers());

        DeltaGetResponse delta = new DeltaGetResponse();
        List<Group> testGroups = getTestGrouplistAddedRemoved(3, 6, 3);
        TestUtils.TestGroupData testGroupData = toTestGroupData(testGroups);
        delta.setValue(testGroups); // 18 adds, 9 removes
        delta.setOdataDeltaLink("delta link");

        for (String group_id: testGroupData.removedMemberships) {
            orchestrator.getUsers().put(group_id, new DBUser(UUID.randomUUID()));
        }
        ReflectionTestUtils.setField(msGraphGroup, "orchestrator", testdata);


        when(deltaRequestBuilder.get(any())).thenReturn(delta);

        msGraphGroup.pullAllGroupsDelta();

        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(azureGroupProducerService, times(3)).processGroup(any(AzureGroup.class));
            verify(azureGroupMembershipProducerService, times(18)).addMembership(any(AzureGroupMembership.class));
            verify(orchestratormemberships, times(9)).remove(anyString());
            verify(azureGroupMembershipProducerService, times(9)).removeMembership(any(AzureGroupMembership.class));
        });
    }

    @Test
    void makeSure18NewUsersAreIgnoredSinceTheyAlreadyAreInCacheAnd9IsremovedFromCacheAndArePublishedAsRemovedOnKafka() {
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute())
                .thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);

        DeltaGetResponse delta = new DeltaGetResponse();
        List<Group> testGroups = getTestGrouplistAddedRemoved(3, 6, 3);
        TestUtils.TestGroupData testGroupData = toTestGroupData(testGroups);
        delta.setValue(testGroups); // 18 adds, 9 removes
        delta.setOdataDeltaLink("delta link");

        for (String group_id: testGroupData.removedMemberships) {
            orchestrator.getUsers().put(group_id, new DBUser(UUID.randomUUID()));
        }
        for (String group_id: testGroupData.createdMemberships) {
            orchestrator.getUsers().put(group_id, new DBUser(UUID.randomUUID()));
        }
        ReflectionTestUtils.setField(msGraphGroup, "orchestrator", orchestrator);


        when(deltaRequestBuilder.get(any())).thenReturn(delta);

        msGraphGroup.pullAllGroupsDelta();

        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(azureGroupProducerService, times(3)).processGroup(any(AzureGroup.class));
            verify(orchestrator.getMemberships(), times(18)).put(anyString(), any(DBMembership.class));
            verify(azureGroupMembershipProducerService, never()).addMembership(any(AzureGroupMembership.class));
            verify(orchestrator.getMemberships(), times(9)).remove(anyString());
            verify(azureGroupMembershipProducerService, times(9)).removeMembership(any(AzureGroupMembership.class));
        });

    }

    @Test
    void makeSurePageThroughGroupsDeltaHandlesZeroGroups() {
        // TEST OK
        when(configGroup.getSuffix()).thenReturn("-suff-");
        lenient().when(configGroup.getGrouppagingsize()).thenReturn(1);
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(configGroup.getFintkontrollidattribute()).thenReturn(
                "extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");

        DeltaGetResponse deltaGetResponseTest = new DeltaGetResponse();
        deltaGetResponseTest.setValue(getTestGrouplist(0,0));
        deltaGetResponseTest.setOdataDeltaLink("delta link");

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);

        when(deltaRequestBuilder.get(any())).thenReturn(deltaGetResponseTest);

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> msGraphGroup.pullAllGroupsDelta()).join();
//        assertTrue(testPool.awaitQuiescence(15, SECONDS));
//
//        assertTrue(ForkJoinPool.commonPool().awaitQuiescence(15, SECONDS));

        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(azureGroupProducerService, times(0)).processGroup(any());
        });
    }

    @Test
    void makeSureDeltaFunctionHandlesNoDeltaLinkIfODataDeltaLinkIsUndefinedOnLastPage() {
        // TEST OK
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute()).thenReturn(
                "extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
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

        DeltaGetResponse lastRecovered = new DeltaGetResponse();
        lastRecovered.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        lastRecovered.setOdataNextLink(null);
        lastRecovered.setOdataDeltaLink("recovered-delta-token");

        when(deltaRequestBuilder.get(any()))
                .thenReturn(firstPage)
                .thenReturn(secondPage)
                .thenReturn(lastPage);

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
        when(deltaRequestBuilder.get(any())).thenReturn(firstPage);
        when(requestAdapter.send(any(RequestInformation.class), any(), any()))
                .thenReturn(secondPage, lastPage);

        when(deltaRequestBuilder.withUrl("LinkToSecondPage")).thenReturn(deltaRequestBuilder);
        when(deltaRequestBuilder.withUrl("LinkToThirdPage")).thenReturn(deltaRequestBuilder);

        when(deltaRequestBuilder.get(any()))
                .thenReturn(firstPage)
                .thenReturn(secondPage)
                .thenReturn(lastPage)
                .thenReturn(lastRecovered);

        msGraphGroup.pullAllGroupsDelta();

        assertNull(lastPage.getOdataDeltaLink(), "Last page should not have a delta link.");
    }

//    @Test
//    void makeSurePageThroughGroupsDeltaReturnsDeltaOnLastPage() {
//        // TEST OK
//        when(configGroup.getSuffix()).thenReturn("-suff-");
//        when(configGroup.getFintkontrollidattribute()).thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
//        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
//
//
//        DeltaGetResponse firstPage = new DeltaGetResponse();
//        firstPage.setValue(getTestGrouplistAddedRemoved(3, 6, 3));
//        firstPage.setOdataNextLink("LinkToSecondPage");
//
//        DeltaGetResponse secondPage = new DeltaGetResponse();
//        secondPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
//        secondPage.setOdataNextLink("LinkToThirdPage");
//
//        DeltaGetResponse thirdPage = new DeltaGetResponse();
//        thirdPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
//        thirdPage.setOdataDeltaLink("delta link");
//
//        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
//        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
//        when(deltaRequestBuilder.get(any()))
//                .thenReturn(firstPage, secondPage, thirdPage);
//
//        when(deltaRequestBuilder.get(any())).thenReturn(firstPage);
//
//        when(requestAdapter.send(any(RequestInformation.class), any(), any()))
//                .thenReturn(secondPage, thirdPage);
//
//        DeltaGetResponse lastPage = new DeltaGetResponse();
//        lastPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
//        lastPage.setOdataNextLink(null);
//        lastPage.setOdataDeltaLink("last delta link");
//
//        when(deltaRequestBuilder.get()).thenReturn(lastPage);
//        when(deltaRequestBuilder.withUrl("LinkToSecondPage")).thenReturn(deltaRequestBuilder);
//        when(deltaRequestBuilder.withUrl("LinkToThirdPage")).thenReturn(deltaRequestBuilder);
//
//        ForkJoinPool testPool = new ForkJoinPool(2);
//        testPool.submit(() -> azureClient.pullAllGroupsDelta()).join();
////        assertTrue(testPool.awaitQuiescence(15, SECONDS));
//
//        await().atMost(15, SECONDS).untilAsserted(() -> {
//
//            verify(azureGroupProducerService, times(4)).processGroup(any());
//            verify(requestAdapter, times(2)).send(any(RequestInformation.class), any(), any());
//            verify(groupsRequestBuilder, times(2)).delta();
//            verify(graphServiceClient, times(2)).groups();
//            verify(deltaRequestBuilder, times(1)).withUrl("LinkToSecondPage");
//            verify(deltaRequestBuilder, times(1)).get(any());
//            verify(deltaRequestBuilder, times(1)).get();
//
//            assertNull(lastPage.getOdataNextLink(), "Last page should not have a next link.");
//            assertNotNull(lastPage.getOdataDeltaLink(), "Last page should have a delta link.");
//            assertEquals("last delta link", lastPage.getOdataDeltaLink(), "Delta link should match expected value.");
//        });
//    }

    @Test
    void makeSurePageThroughGroupsDeltaReturnsDeltaOnLastPage() {
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute())
                .thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);

        DeltaGetResponse firstPage = new DeltaGetResponse();
        firstPage.setValue(getTestGrouplistAddedRemoved(3, 6, 3));
        firstPage.setOdataNextLink("LinkToSecondPage");

        DeltaGetResponse secondPage = new DeltaGetResponse();
        secondPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        secondPage.setOdataNextLink("LinkToThirdPage");

        DeltaGetResponse thirdPage = new DeltaGetResponse();
        thirdPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        thirdPage.setOdataDeltaLink("delta link"); // final
        DeltaRequestBuilder page2Builder      = mock(DeltaRequestBuilder.class);
        DeltaRequestBuilder page3Builder      = mock(DeltaRequestBuilder.class);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
        when(deltaRequestBuilder.get(any())).thenReturn(firstPage);
        when(deltaRequestBuilder.withUrl("LinkToSecondPage"))
                .thenReturn(page2Builder);
        when(page2Builder.get())
                .thenReturn(secondPage);
        when(deltaRequestBuilder.withUrl("LinkToThirdPage"))
                .thenReturn(page3Builder);
        when(page3Builder.get())
                .thenReturn(thirdPage);

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> msGraphGroup.pullAllGroupsDelta()).join();

        await().atMost(15, SECONDS).untilAsserted(() -> {
            verify(groupsRequestBuilder, atLeastOnce()).delta();
            verify(deltaRequestBuilder, times(1)).get(any());
            verify(page2Builder, times(1)).get();
            verify(page3Builder, times(1)).get();
            verify(deltaRequestBuilder, times(1)).withUrl("LinkToSecondPage");
            verify(deltaRequestBuilder, times(1)).withUrl("LinkToThirdPage");
            assertNull(thirdPage.getOdataNextLink());
            assertEquals("delta link", thirdPage.getOdataDeltaLink());
        });

    }

    @Test
    void makeSurePageThroughGroupsDeltaPagesThroughPages() {
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute())
                .thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");

        DeltaGetResponse firstPage = new DeltaGetResponse();
        firstPage.setValue(getTestGrouplistAddedRemoved(3, 6, 3));
        firstPage.setOdataNextLink("LinkToSecondPage");

        DeltaGetResponse secondPage = new DeltaGetResponse();
        secondPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        secondPage.setOdataNextLink("LinkToThirdPage");

        DeltaGetResponse thirdPage = new DeltaGetResponse();
        thirdPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        thirdPage.setOdataNextLink(null);
        thirdPage.setOdataDeltaLink("delta link");

        DeltaRequestBuilder page2Builder      = mock(DeltaRequestBuilder.class);
        DeltaRequestBuilder page3Builder      = mock(DeltaRequestBuilder.class);

        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
        when(deltaRequestBuilder.get(any()))
                .thenReturn(firstPage);

        when(deltaRequestBuilder.withUrl("LinkToSecondPage"))
                .thenReturn(page2Builder);
        when(page2Builder.get())
                .thenReturn(secondPage);
        when(deltaRequestBuilder.withUrl("LinkToThirdPage"))
                .thenReturn(page3Builder);
        when(page3Builder.get())
                .thenReturn(thirdPage);

        new ForkJoinPool(2).submit(() -> msGraphGroup.pullAllGroupsDelta()).join();

        verify(graphServiceClient, atLeastOnce()).groups();
        verify(groupsRequestBuilder, atLeastOnce()).delta();
        verify(deltaRequestBuilder, times(1)).get(any());
        verify(page2Builder, times(1)).get();
        verify(page3Builder, times(1)).get();
        verify(deltaRequestBuilder, times(1)).withUrl("LinkToSecondPage");
        verify(deltaRequestBuilder, times(1)).withUrl("LinkToThirdPage");

        verify(azureGroupProducerService, times(4)).processGroup(any()); // adjust if needed
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
        com.microsoft.graph.users.delta.DeltaRequestBuilder userDeltaRequestBuilder = mock(com.microsoft.graph.users.delta.DeltaRequestBuilder.class);

        when(usersRequestBuilder.delta()).thenReturn(userDeltaRequestBuilder);

        OnPremisesExtensionAttributes onPremAttributes = new OnPremisesExtensionAttributes();
        onPremAttributes.setExtensionAttribute10("1236");
        User user = new User();
        user.setId("1234");
        user.setMail("testuser1@mail.com");
        user.setUserPrincipalName("testuser1@mail.com");
        user.setAccountEnabled(true);
        user.setOnPremisesExtensionAttributes(onPremAttributes);
        user.setEmployeeId("1236");

        OnPremisesExtensionAttributes onPremAttributes2 = new OnPremisesExtensionAttributes();
        onPremAttributes2.setExtensionAttribute10("4566");
        User user2 = new User();
        user2.setId("4565");
        user2.setMail("testuser2@mail.com");
        user2.setUserPrincipalName("testuser2@mail.com");
        user2.setAccountEnabled(true);
        user2.setOnPremisesExtensionAttributes(onPremAttributes2);
        user2.setUserType("Member");
        user2.setEmployeeId("1236");


        User extUser = new User();
        extUser.setId("7896");
        extUser.setMail("testExtuser2@mail.com");
        extUser.setUserPrincipalName("testExtuser2@mail.com");
        extUser.setAccountEnabled(true);
        OnPremisesExtensionAttributes onPremAttributes3 = new OnPremisesExtensionAttributes();
        onPremAttributes3.setExtensionAttribute11("novari");
        extUser.setOnPremisesExtensionAttributes(onPremAttributes3);
        extUser.setUserType("Member");
        extUser.setEmployeeId("1236");

        List<User> userList = new ArrayList<>();
        userList.add(user);
        userList.add(user2);
        userList.add(extUser);

        com.microsoft.graph.users.delta.DeltaGetResponse firstPage = new com.microsoft.graph.users.delta.DeltaGetResponse();
        firstPage.setValue(userList);
        when(userDeltaRequestBuilder.get(any())).thenReturn(firstPage);
        when(configUser.getEnableExternalUsers()).thenReturn(true);
        when(configUser.getUserpagingsize()).thenReturn(1000);

        AzureUser cachedUser = new AzureUser(user, configUser);
        AzureUser nonCachedUser = new AzureUser(user2, configUser);
        lenient().when(orchestrator.getUsers().containsKey(user.getId())).thenReturn(true);
        lenient().when(orchestrator.getUsers().get(user.getId())).thenReturn(DBUserMapper.toDBUser(cachedUser));

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> msGraphUser.pullAllUsersDelta()).join();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(azureUserProducerService, never()).publish(cachedUser);
            verify(azureUserProducerService, times(1)).publish(nonCachedUser);
            verify(azureUserExternalProducerService, times(1)).publish(any(AzureUserExternal.class));
        });

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
        lenient().when(orchestrator.getUsers().containsKey(user.getId())).thenReturn(true);
        lenient().when(orchestrator.getUsers().get(user.getId())).thenReturn(DBUserMapper.toDBUser(cachedUser));

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> msGraphUser.pullAllUsersDelta()).join();
//        assertTrue(testPool.awaitQuiescence(15, SECONDS));

        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(azureUserProducerService, times(0)).publish(notCachedUser);
        });
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
        testPool.submit(() -> msGraphGroup.pullAllGroupsDelta()).join();
        assertTrue(testPool.awaitQuiescence(15, SECONDS));


        //assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, SECONDS));
        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(azureGroupMembershipProducerService, times(9)).addMembership(any(AzureGroupMembership.class));
        });

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
        com.microsoft.graph.users.delta.DeltaRequestBuilder userDeltaRequestBuilder = mock(com.microsoft.graph.users.delta.DeltaRequestBuilder.class);

        when(usersRequestBuilder.delta()).thenReturn(userDeltaRequestBuilder);

        OnPremisesExtensionAttributes onPremAttributes = new OnPremisesExtensionAttributes();
        onPremAttributes.setExtensionAttribute10("123");
        User user = new User();
        String userId = UUID.randomUUID().toString();
        user.setId(userId);
        user.setMail("testuser1@mail.com");
        user.setUserPrincipalName("testuser1@mail.com");
        user.setAccountEnabled(true);
        user.setOnPremisesExtensionAttributes(onPremAttributes);
        user.setUserType("Member");
        user.setEmployeeId("1236");


        OnPremisesExtensionAttributes onPremAttributes2 = new OnPremisesExtensionAttributes();
        onPremAttributes2.setExtensionAttribute10("456");
        User user2 = new User();
        user2.setId("456");
        user2.setMail("testuser2@mail.com");
        user2.setUserPrincipalName("testuser2@mail.com");
        user2.setAccountEnabled(true);
        user2.setOnPremisesExtensionAttributes(onPremAttributes2);
        user2.setUserType("Member");
        user2.setEmployeeId("1236");


        List<User> userList = new ArrayList<>();
        userList.add(user);
        userList.add(user2);

        com.microsoft.graph.users.delta.DeltaGetResponse firstPage = new com.microsoft.graph.users.delta.DeltaGetResponse();
        firstPage.setValue(userList);
        when(userDeltaRequestBuilder.get(any())).thenReturn(firstPage);

        AzureUser cachedUser = new AzureUser(user, configUser);
        AzureUser notCachedUser = new AzureUser(user2, configUser);
        DBUser dbUser = DBUserMapper.toDBUser(cachedUser);
        DBObjectList<DBUser> dbObjectList = new DBObjectList<>();
        dbObjectList.put(dbUser.getId().toString(), dbUser);
        when(orchestrator.getUsers()).thenReturn(dbObjectList);

//        lenient().when(orchestrator.getUsers().containsKey(user.getId())).thenReturn(true);
//        lenient().when(orchestrator.getUsers().get(user.getId())).thenReturn(DBUserMapper.toDBUser(cachedUser));

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> msGraphUser.pullAllUsersDelta()).join();
        //assertTrue(testPool.awaitQuiescence(15, SECONDS));
        await().atMost(15, SECONDS).untilAsserted(() -> {
            verify(azureUserProducerService, times(1)).publish(notCachedUser);
        });

    }

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

             msGraphGroup.addGroupMembership(resourceGroupMembership, kafkaKey);

             await().atMost(5, SECONDS).untilAsserted(() -> {
             verify(azureGroupMembershipProducerService, timeout(5000).times(1)).addMembership(any(AzureGroupMembership.class));
             });
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

         msGraphGroup.addGroupMembership(resourceGroupMembership, kafkaKey);

         //assertTrue(ForkJoinPool.commonPool().awaitQuiescence(5, SECONDS));
         await().atMost(5, SECONDS).untilAsserted(() -> {
             verify(azureGroupMembershipProducerService, timeout(5000).times(0)).addMembership(any(AzureGroupMembership.class));
             verify(azureGroupMembershipProducerService, timeout(5000).times(0)).removeMembership(any(AzureGroupMembership.class));
         });
     }

    @Test
    void makeSureHTTPDeleteIsCalledWhenDeleteGroupAsyncIsCalled() {
        Group g = new Group();
        g.setId("delGroupID");
        g.getAdditionalData().put("fintkontrollId", "delGroupID"); // the attribute your code checks

        GroupCollectionResponse page = new GroupCollectionResponse();
        page.setValue(List.of(g));

        when(groupsRequestBuilder.get(any(Consumer.class))).thenReturn(page);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
        when(groupCollectionResponse.getValue()).thenReturn(List.of(g));
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute()).thenReturn("fintkontrollId");

        ForkJoinPool testPool = new ForkJoinPool(2);
        testPool.submit(() -> msGraphGroup.deleteGroupAsync(g.getId())).join();

        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(groupItemRequestBuilder, times(1)).delete();
        });
    }


     @Test
     void makeSureHTTPDeleteIsCalledWhenDeleteGroupIsCalled() {
         Group g = new Group();
         g.setId("delGroupID");
         g.getAdditionalData().put("fintkontrollId", "delGroupID"); // the attribute your code checks

         GroupCollectionResponse page = new GroupCollectionResponse();
         page.setValue(List.of(g));

         when(groupsRequestBuilder.get(any(Consumer.class))).thenReturn(page);
         when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
         when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
         when(groupCollectionResponse.getValue()).thenReturn(List.of(g));
         when(configGroup.getSuffix()).thenReturn("-suff-");
         when(configGroup.getFintkontrollidattribute()).thenReturn("fintkontrollId");

         msGraphGroup.deleteGroupAsync(g.getId());
         await().atMost(5, SECONDS).untilAsserted(() -> {
             verify(groupItemRequestBuilder, times(1)).delete();
         });
     }

    @Test
    void multiplePagesWhenDeletingSingleGroupShouldThrowError() {
        Group g1 = new Group();
        g1.setId("delGroupID123");
        g1.getAdditionalData().put("fintkontrollId", "refGroupID");

        Group g2 = new Group();
        g2.setId("delGroupID456");
        g2.getAdditionalData().put("fintkontrollId", "refGroupID");

        GroupCollectionResponse page = new GroupCollectionResponse();
        page.setValue(List.of(g1,g2));

        when(groupsRequestBuilder.get(any(Consumer.class))).thenReturn(page);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.byGroupId(anyString())).thenReturn(groupItemRequestBuilder);
        when(groupCollectionResponse.getValue()).thenReturn(List.of(g1,g2));
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute()).thenReturn("fintkontrollId");

        Logger logger = (Logger) LoggerFactory.getLogger(MsGraphGroup.class);
        ListAppender<ILoggingEvent> app = new ListAppender<>();
        app.start();
        logger.addAppender(app);

        msGraphGroup.deleteGroupAsync("refGroupID");

        await().atMost(5, SECONDS).untilAsserted(() -> {
            assertTrue(app.list.stream().anyMatch(e ->
                    e.getLevel() == Level.ERROR &&
                            e.getFormattedMessage().contains("Expected exactly 1 group, found 2") &&
                            e.getFormattedMessage().contains("fintkontrollId=refGroupID")
            ));

            verify(groupsRequestBuilder, never()).byGroupId(anyString());
            verify(groupItemRequestBuilder, never()).delete();
        });

        logger.detachAppender(app);
    }

    @Test
    public void makeSureGetNextPageIsCalledAsExpected() {
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute())
                .thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");

        DeltaGetResponse firstPage = new DeltaGetResponse();
        firstPage.setValue(getTestGrouplistAddedRemoved(3, 6, 3));
        firstPage.setOdataNextLink("LinkToSecondPage");

        DeltaGetResponse secondPage = new DeltaGetResponse();
        secondPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        secondPage.setOdataNextLink("LinkToThirdPage");

        DeltaGetResponse thirdPage = new DeltaGetResponse();
        thirdPage.setValue(getTestGrouplistAddedRemoved(4, 2, 1));
        thirdPage.setOdataNextLink(null);
        thirdPage.setOdataDeltaLink("delta link");

        DeltaRequestBuilder deltaRequestBuilder = mock(DeltaRequestBuilder.class);
        DeltaRequestBuilder page2Builder      = mock(DeltaRequestBuilder.class);
        DeltaRequestBuilder page3Builder      = mock(DeltaRequestBuilder.class);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);

        when(deltaRequestBuilder.get(any()))
                .thenReturn(firstPage);
        when(deltaRequestBuilder.withUrl("LinkToSecondPage"))
                .thenReturn(page2Builder);
        when(page2Builder.get())
                .thenReturn(secondPage);
        when(deltaRequestBuilder.withUrl("LinkToThirdPage"))
                .thenReturn(page3Builder);
        when(page3Builder.get())
                .thenReturn(thirdPage);
        new ForkJoinPool(2).submit(() -> msGraphGroup.pullAllGroupsDelta()).join();

        verify(graphServiceClient, atLeastOnce()).groups();
        verify(groupsRequestBuilder, atLeastOnce()).delta();

        verify(deltaRequestBuilder, times(1)).get(any());
        verify(page2Builder, times(1)).get();
        verify(page3Builder, times(1)).get();
        verify(deltaRequestBuilder, times(1)).withUrl("LinkToSecondPage");
        verify(deltaRequestBuilder, times(1)).withUrl("LinkToThirdPage");
        assertNull(thirdPage.getOdataNextLink(), "Last page should not have a next link.");
        assertEquals("delta link", thirdPage.getOdataDeltaLink(), "Delta link should match expected value.");
    }

    @Test
    public void makeSureTrownErrorIsSwallowedAndNotThrown() {
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute())
                .thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
        when(deltaRequestBuilder.get(ArgumentMatchers.any())).thenThrow(new ApiException("Test exception"));

        msGraphGroup.pullAllGroupsDelta();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(azureGroupProducerService, never()).processGroup(any(AzureGroup.class));
            verify(azureGroupMembershipProducerService, never()).addMembership(any(AzureGroupMembership.class));
            verify(azureGroupMembershipProducerService, never()).removeMembership(any(AzureGroupMembership.class));
            verify(azureGroupMembershipCache, never());
        });
        assertDoesNotThrow(()-> {
            msGraphGroup.pullAllGroupsDelta();
        });
    }


    @Test
    public void shouldHandleTimeoutException() {

        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);
        when(deltaRequestBuilder.get(ArgumentMatchers.any())).thenThrow(new ApiException("Gateway Timeout"));

        msGraphGroup.pullAllGroupsDelta();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(azureGroupProducerService, never()).processGroup(any(AzureGroup.class));
            verify(azureGroupMembershipProducerService, never()).addMembership(any(AzureGroupMembership.class));
            verify(azureGroupMembershipProducerService, never()).removeMembership(any(AzureGroupMembership.class));
            verify(azureGroupMembershipCache, never());
        });
    }

    @Test
    void makeSure10GroupsWith1000UsersCreateCacheWith10000MembershipsAndPostsAllMembershipsToKafka()
    {
        int groups = 10;
        int membersPrGroups = 1000;
        when(configGroup.getSuffix()).thenReturn("-suff-");
        when(configGroup.getFintkontrollidattribute())
                .thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
        when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
        when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
        when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);

        DeltaGetResponse delta = new DeltaGetResponse();
        delta.setValue(getTestGrouplistAddedRemoved(groups, membersPrGroups, 0)); // 10 groups, 1000 adds/group, 0 removes
        delta.setOdataDeltaLink("delta link");
        when(deltaRequestBuilder.get(any())).thenReturn(delta);

        msGraphGroup.pullAllGroupsDelta();

        await().atMost(60, SECONDS).untilAsserted(() -> {
            verify(azureGroupProducerService, times(groups)).processGroup(any(AzureGroup.class));
            verify(azureGroupMembershipProducerService, times(groups*membersPrGroups)).addMembership(any(AzureGroupMembership.class));
            verify(azureGroupMembershipProducerService, never()).removeMembership(any(AzureGroupMembership.class));
            verify(azureGroupMembershipCache, times(groups*membersPrGroups));
        });
    }

    @Test
    @Disabled("This function should only be launched manually")
    void makeSure1000GroupsWith2000UsersCreateCacheWith20MillMembershipsAndPostsAllMembershipsToKafka()
    {
        {
            int groups = 1000;
            int membersPrGroups = 20000;
            when(configGroup.getSuffix()).thenReturn("-suff-");
            when(configGroup.getFintkontrollidattribute())
                    .thenReturn("extension_be2ffab7d262452b888aeb756f742377_FintKontrollRoleId");
            when(graphServiceClient.getRequestAdapter()).thenReturn(requestAdapter);
            when(graphServiceClient.groups()).thenReturn(groupsRequestBuilder);
            when(groupsRequestBuilder.delta()).thenReturn(deltaRequestBuilder);

            DeltaGetResponse delta = new DeltaGetResponse();
            delta.setValue(getTestGrouplistAddedRemoved(groups, membersPrGroups, 0));
            delta.setOdataDeltaLink("delta link");
            when(deltaRequestBuilder.get(any())).thenReturn(delta);

            msGraphGroup.pullAllGroupsDelta();

            await().atMost(5, MINUTES).untilAsserted(() -> {
                verify(azureGroupProducerService, times(groups)).processGroup(any(AzureGroup.class));
                verify(azureGroupMembershipProducerService, times(groups*membersPrGroups)).addMembership(any(AzureGroupMembership.class));
                verify(azureGroupMembershipProducerService, never()).removeMembership(any(AzureGroupMembership.class));
            });
        }
    }
}