package no.fintlabs;

import com.microsoft.graph.core.tasks.PageIterator;
import com.microsoft.graph.groups.delta.DeltaGetResponse;
import com.microsoft.graph.groups.delta.DeltaRequestBuilder;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.serialization.UntypedArray;
import com.microsoft.kiota.serialization.UntypedObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.azure.*;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.ResourceGroupMembership;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.Consumer;


@Component
@Log4j2
@RequiredArgsConstructor

public class
AzureClient {
    protected final Config config;
    protected final ConfigGroup configGroup;
    protected final ConfigUser configUser;
    protected final GraphServiceClient graphServiceClient;
    private final ConcurrentHashMap<String, AzureUser> entraIdUserCache;
    private final ConcurrentHashMap<String, AzureUserExternal> entraIdExternalUserCache;
    private final ConcurrentHashMap<String, Optional<ResourceGroupMembership>> resourceGroupMembershipCache;
    private final ConcurrentHashMap<String, AzureGroup> azureGroupCache;
    private final HashSet<String> azureGroupMembershipCache;
    private final AzureUserProducerService azureUserProducerService;
    private final AzureUserExternalProducerService azureUserExternalProducerService;
    private final AzureGroupProducerService azureGroupProducerService;
    private final AzureGroupMembershipProducerService azureGroupMembershipProducerService;
    private final ExecutorService executor = Executors.newFixedThreadPool(40);
    private String odataDeltaLink;
    private AtomicInteger numMembers;
    private AtomicInteger groupCounter;
    private Set<String> processedGroupIds;

    @Scheduled(cron = "${fint.kontroll.azure-ad-gateway.group-scheduler.clear-cache}")
    public void clearCaches() {
        odataDeltaLink = null;
        entraIdUserCache.clear();
        entraIdExternalUserCache.clear();
        azureGroupCache.clear();
        azureGroupMembershipCache.clear();
            log.info("Delta caches for group and user has been reset to null due to scheduler. Next call will try to fetch all users and groups from Entra ID");
    }

    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.fixed-delay-ms}"
    )
    public void pullAllUsers() {
        log.info("*** <<< Starting to pull users from Microsoft Graph >>> ***");
        long startTime = System.currentTimeMillis();
        String[] selectionCriteria = new String[]{String.join(",", configUser.AllAttributes())};
        String filterCriteria = "usertype eq 'member'";
        try {
            this.pageThroughUsers(graphServiceClient.users()
                    .get(requestConfiguration -> {
                        requestConfiguration.queryParameters.select = selectionCriteria;
                        requestConfiguration.queryParameters.filter = filterCriteria;
                        requestConfiguration.queryParameters.top = configUser.getUserpagingsize();
                    }));
        } catch (ApiException | ReflectiveOperationException ex) {
            log.error("pullAllUsers failed with message: {}", ex.getMessage());
        }
        long endTime = System.currentTimeMillis();
        long elapsedTimeInSeconds = (endTime - startTime) / 1000;
        long minutes = elapsedTimeInSeconds / 60;
        long seconds = elapsedTimeInSeconds % 60;

        log.info("*** <<< Finished pulling users from Microsoft Graph in {} minutes and {} seconds >>> *** ", minutes, seconds);
    }

    private void pageThroughUsers(UserCollectionResponse userPage) throws ReflectiveOperationException {
        AtomicInteger users = new AtomicInteger();
        AtomicInteger changedUsers = new AtomicInteger();
        AtomicInteger changedExtUsers = new AtomicInteger();
        PageIterator<User, UserCollectionResponse> pageIterator = new PageIterator.Builder<User, UserCollectionResponse>()
                .client(graphServiceClient)
                .collectionPage(userPage)
                .collectionPageFactory(UserCollectionResponse::createFromDiscriminatorValue)
                .processPageItemCallback(user -> {
                    users.getAndIncrement();

                    if (entraIdUserCache != null &&
                            entraIdUserCache.containsKey(user.getId())) {
                        AzureUser entraIdUserObject = new AzureUser(user, configUser);
                        if (entraIdUserObject.equals(entraIdUserCache.get(user.getId()))) {
                            log.info("User {} is unchanged from Entra Cache. Skipping publishing to Kafka.", user.getId());
                            return true;
                        }
                    }

                    String externalUserAttribute = AzureUser.getAttributeValue(user, configUser.getExternaluserattribute());
                    if (configUser.getEnableExternalUsers() && externalUserAttribute != null
                            && externalUserAttribute.equalsIgnoreCase(configUser.getExternaluservalue())) {
                        AzureUserExternal entraUserExtObject = new AzureUserExternal(user, configUser);
                        if (entraIdExternalUserCache != null &&
                                entraIdExternalUserCache.containsKey(user.getId()) && entraUserExtObject.equals(entraIdExternalUserCache.get(user.getId()))) {
                            log.info("External User {} is unchanged. Skipping publishing to Kafka.", user.getId());
                            return true;
                        }
                        else {
                            log.info("Publishing external user to Kafka: {}", user.getUserPrincipalName());
                            azureUserExternalProducerService.publish(new AzureUserExternal(user, configUser));
                            changedExtUsers.getAndIncrement();
                            entraIdExternalUserCache.put(user.getId(), new AzureUserExternal(user, configUser));
                        }
                    } else {
                        AzureUser azureuser = new AzureUser(user, configUser);
                        if ((azureuser.getEmployeeId() != null && !azureuser.getEmployeeId().isEmpty()) ||
                                (azureuser.getStudentId() != null && !azureuser.getStudentId().isEmpty())) {
                            log.info("Publishing user to Kafka: {}", user.getUserPrincipalName());
                            azureUserProducerService.publish(azureuser);
                            log.info("Updating cache for user: {}", user.getId());
                            changedUsers.getAndIncrement();
                            entraIdUserCache.put(user.getId(), azureuser);
                        } else {
                            log.warn("UserId: {} does not contain required employeeId or studentId. Not published to kafka", user.getId());
                        }
                    }
                    return true;
                }).build();

        pageIterator.iterate();
        if (odataDeltaLink != null) {
            if(changedUsers.get() > 0) {
                log.info("*** <<< Found total {} users in Entra ID. Published {} changed users to Kafka >>> ***", users.get(), changedUsers.get());
            }
            else {
                    log.info("*** <<< No changes since last call on users from graph >>> ***");
                }
            if (changedExtUsers.get() > 0) {
                log.info("*** <<< Found {} users of type External users in Entra ID that were changed >>> ***", changedExtUsers.get());
            }
        } else {
            if(changedUsers.get() > 0) {
                log.info("*** <<< Found total {} users in Entra ID. {} published to Kafka >>> ***", users.get(), changedUsers.get());
            }
            else {
                log.info("*** <<< No changes since last call on users from graph >>> ***");
            }
            if (changedExtUsers.get() > 0) {
                log.info("*** <<< Of the total, there are {} users of type External users in Entra ID >>> ***", changedExtUsers.get());
            }
            else {
                log.info("*** <<< No changes on external users since last call on users from graph >>> ***");
            }
        }
    }

//    @Scheduled(
//            initialDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.delta-pull.initial-delay-ms}",
//            fixedDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.delta-pull.delta-delay-ms}"
//    )
    public void pullAllGroupsDelta() {
        log.info("*** <<< Fetching groups and members using delta call from Microsoft Graph >>> ***");
        String[] selectionCriteria = new String[]{String.format("id,displayName,description,members,%s", configGroup.getFintkontrollidattribute())};
        numMembers = new AtomicInteger(0);
        processedGroupIds = new HashSet<>();
        long groupStartTime = System.currentTimeMillis();

        try {

            Consumer<DeltaRequestBuilder.GetRequestConfiguration> configureRequest = requestConfiguration -> {
                requestConfiguration.queryParameters.select = selectionCriteria;
                requestConfiguration.queryParameters.top = configGroup.getGrouppagingsize();
            };

            DeltaGetResponse groupPage = (odataDeltaLink != null)
                    ? graphServiceClient.groups().delta().withUrl(odataDeltaLink).get(configureRequest)
                    : graphServiceClient.groups().delta().get(configureRequest);

            pageThroughGroupsDelta(groupPage);

        } catch (ApiException | ReflectiveOperationException e) {
            log.error("Failed when trying to get groups. ", e);
        }
        long endTime = System.currentTimeMillis();
        long elapsedTimeInSeconds = (endTime - groupStartTime) / 1000;
        long minutes = elapsedTimeInSeconds / 60;
        long seconds = elapsedTimeInSeconds % 60;

        if (processedGroupIds.size() > 0 || numMembers.get() > 0) {
            log.info("*** <<< Found {} groups with suffix \"{}\" that included {} memberships, published to Kafka, in {} minutes and {} seconds  >>> ***",
                    processedGroupIds.size(),
                    configGroup.getSuffix(),
                    numMembers.get(),
                    minutes,
                    seconds);
        } else {
            log.info("*** <<< No changes since last delta call on groups from Graph. Finished in {} minutes and {} seconds >>> ***",
                    minutes,
                    seconds);
        }

    }

    private void pageThroughGroupsDelta(DeltaGetResponse groupPage) throws ReflectiveOperationException {

        while (true) {
            deltaPageIterator(groupPage);
            if (groupPage != null && groupPage.getOdataNextLink() != null) {
                groupPage = graphServiceClient.groups().delta().withUrl(groupPage.getOdataNextLink()).get();
            } else {
                break;
            }
        }

        if (groupPage.getOdataDeltaLink() == null) {
            log.error("Logic error: Last page doesn't contain ODataDeltaLink");
            throw new ReflectiveOperationException("Logic error: Last page doesn't contain ODataDeltaLink");
        }

        if(odataDeltaLink == null) {
            log.info("*** <<< Initial Delta run on Groups completed >>> ***");
        }
        odataDeltaLink = groupPage.getOdataDeltaLink();
        log.info("Delta link updated in variable odataDeltaLink. Finished pullAllGroupsDelta");
    }

    private void deltaPageIterator(DeltaGetResponse groupPage) throws ReflectiveOperationException {
        Set<String> currentPageProcessedGroupIds = new HashSet<>();

        PageIterator<Group, DeltaGetResponse> pageIterator = new PageIterator.Builder<Group, DeltaGetResponse>()
                .client(graphServiceClient)
                .collectionPage(groupPage)
                .collectionPageFactory(DeltaGetResponse::createFromDiscriminatorValue)
                .processPageItemCallback(group -> {
                    try {
                        String groupId = group.getId();
                        if (currentPageProcessedGroupIds.add(groupId)) {
                            if (group.getDisplayName() != null && group.getDisplayName().endsWith(configGroup.getSuffix())
                                    && !group.getAdditionalData().isEmpty()
                                    && group.getAdditionalData().containsKey(configGroup.getFintkontrollidattribute())) {

                                if(processedGroupIds.add(groupId)) {
                                    AzureGroup newGroup = new AzureGroup(group, configGroup);
                                    azureGroupProducerService.processGroup(newGroup);
                                }

                                log.info("Processing members for group: {}", group.getDisplayName());
                                processMembersDelta(group);
                            } else {
                                log.warn("Skipping group: {} due to missing suffix or attributes", groupId);
                            }
                        } else {
                            log.debug("Group {} already processed in this page", groupId);
                        }
                    } catch (Exception e) {
                        log.error("Error processing group: {}. Skipping to next.", group.getId(), e);
                    }
                    return true;
                }).build();
        pageIterator.iterate();
    }

    private void processMembersDelta(Group group) {
        Map<String, Object> additionalData = group.getAdditionalData();
        if (!additionalData.containsKey("members@delta")) {
            return;
        }
        try {
            Object membersDeltaObject = additionalData.get("members@delta");
            UntypedArray membersDeltaArray = (UntypedArray) membersDeltaObject;
            for (Object member : membersDeltaArray.getValue()) {
                UntypedObject untypedMember = (UntypedObject) member;
                String memberType = (String) untypedMember.getValue().get("@odata.type").getValue();
                String memberId = (String) untypedMember.getValue().get("id").getValue();

                if (!memberType.equals("#microsoft.graph.user")) {
                    continue;
                }

                String kafkaKey = group.getId() + "_" + memberId;
                if (untypedMember.getValue().containsKey("@removed")) {
                    azureGroupMembershipProducerService.removeMembership(new AzureGroupMembership(memberId,group.getId(),kafkaKey));
                    //azureGroupMembershipProducerService.publishDeletedMembership(kafkaKey);
                    resourceGroupMembershipCache.remove(kafkaKey);
                    log.info("Produced message to Kafka on removed user with ObjectID: {} from group: {}", memberId, group.getId());
                    if(odataDeltaLink != null) {
                        log.info("UserId: {} is removed as member from GroupId: {}", memberId, group.getId());
                    }
                    continue;
                }
                azureGroupMembershipProducerService.addMembership(new AzureGroupMembership(memberId,group.getId(),kafkaKey));
                //azureGroupMembershipProducerService.publishAddedMembership(new AzureGroupMembership(memberId,group.getId(),kafkaKey));
                numMembers.getAndIncrement();
                log.debug("Produced message to Kafka where userId: {} is member of groupId: {}", memberId, group.getId());
                if(odataDeltaLink != null) {
                    log.info("UserId: {} is member of GroupId: {}", memberId, group.getId());
                }
            }

        } catch (ClassCastException e) {
            log.error("Failed to process members@delta. Error: {}", e.getMessage());
        }
    }

//  TODO: Consider if this is needed
    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.delta-delay-ms}"
    )
    public void pullAllGroupsAsync() {
        log.info("*** <<< Fetching groups from Microsoft Graph >>> ***");
        long startTime = System.currentTimeMillis();
        numMembers = new AtomicInteger(0);
        groupCounter = new AtomicInteger(0);
        String[] selectionCriteria = new String[]{String.format("id,displayName,description,%s", configGroup.getFintkontrollidattribute())};

        CompletableFuture.supplyAsync(() -> {
            try {
                return pageThroughGroups(graphServiceClient.groups()
                        .get(requestConfiguration -> {
                            requestConfiguration.queryParameters.select = selectionCriteria;
                            requestConfiguration.queryParameters.top = configGroup.getGrouppagingsize();
                        }));
            } catch (ApiException | ReflectiveOperationException e) {
                log.error("Failed when trying to get groups. ", e);
                return new ArrayList<AzureGroup>();
            }
        }, executor).thenAccept(allGroups -> {
            long endTime = System.currentTimeMillis();
            long elapsedTimeInSeconds = (endTime - startTime) / 1000;
            long minutes = elapsedTimeInSeconds / 60;
            long seconds = elapsedTimeInSeconds % 60;
            log.info("*** <<< {} groups with suffix \"{}\" processed in {} minutes and {} seconds. Continuing processing memberships >>> ***", groupCounter, configGroup.getSuffix(), minutes, seconds);
            fetchAndPublishMembersForAllGroupsAsync(allGroups);

        }).exceptionally(ex -> {
            log.error("An error occurred while fetching groups: {}", ex.getMessage());
            return null;
        }).thenRun(() -> {
            long endTime = System.currentTimeMillis();
            long elapsedTimeInSeconds = (endTime - startTime) / 1000;
            long minutes = elapsedTimeInSeconds / 60;
            long seconds = elapsedTimeInSeconds % 60;
            log.info("*** <<< Done processing memberships in {} minutes and {} seconds >>> ***", minutes, seconds);

            if (numMembers.get() > 0) {
                log.info("*** <<< {} Entra group memberships published to kafka as they were not in membership cache >>> ***", numMembers.get());
            } else {
                log.info("*** <<< All entra group members already in cache. No members published to kafka >>> ***");
            }
        }).join();
    }

    private List<AzureGroup> pageThroughGroups(GroupCollectionResponse groupPage) throws ReflectiveOperationException {
        List<AzureGroup> allGroups = new ArrayList<>();

        PageIterator<Group, GroupCollectionResponse> pageIterator = new PageIterator.Builder<Group, GroupCollectionResponse>()
                .client(graphServiceClient)
                .collectionPage(groupPage)
                .collectionPageFactory(GroupCollectionResponse::createFromDiscriminatorValue)
                .processPageItemCallback(group -> {
                    boolean shouldProcess = group.getDisplayName() != null
                            && group.getDisplayName().endsWith(configGroup.getSuffix())
                            && group.getAdditionalData() != null
                            && group.getAdditionalData().containsKey(configGroup.getFintkontrollidattribute());

                    if (!shouldProcess) return true;

                    AzureGroup newGroup = new AzureGroup(group, configGroup);
                    if (azureGroupCache != null
                            && azureGroupCache.containsKey(newGroup.getId())
                            && newGroup.equals(azureGroupCache.get(newGroup.getId()))) {
                        log.info("{} groupID already published and in cache. Not republished to kafka", newGroup.getId());
                    } else {
                        groupCounter.incrementAndGet();
                        azureGroupProducerService.processGroup(newGroup);
                        azureGroupCache.put(newGroup.getId(), newGroup);
                        allGroups.add(newGroup);
                    }
                    return true;
                }).build();

        pageIterator.iterate();
        return allGroups;
    }

    public void fetchAndPublishMembersForAllGroupsAsync(List<AzureGroup> groups) {
        List<CompletableFuture<Void>> memberFutures = groups.stream()
                .map(group -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return graphServiceClient.groups()
                                .byGroupId(group.getId())
                                .members()
                                .get(requestConfiguration -> {
                                    requestConfiguration.queryParameters.select = new String[]{"id"};
                                    requestConfiguration.queryParameters.top = configGroup.getGrouppagingsize();
                                });
                    } catch (Exception e) {
                        log.error("Error fetching members for group {}: {}", group.getId(), e.getMessage());
                        return null;
                    }
                }, executor).thenCompose(memberPage -> {
                    if (memberPage != null) {
                        return pageThroughAzureGroupAsync(group, memberPage);
                    }
                    return CompletableFuture.completedFuture(null);
                }))
                .toList();

        CompletableFuture.allOf(memberFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("*** <<< Finished fetching members for all groups >>> ***"))
                .join();
    }

    private CompletableFuture<Void> pageThroughAzureGroupAsync(AzureGroup azureGroup,
                                                               DirectoryObjectCollectionResponse inPage) {
        AtomicInteger membersPerGroupCount = new AtomicInteger(0);

        return processPageAsync(azureGroup, inPage, membersPerGroupCount)
                .thenRun(() -> log.info("{} memberships detected in groupName \"{}\" with groupId {}",
                        membersPerGroupCount.get(), azureGroup.getDisplayName(), azureGroup.getId()));
    }

    private CompletableFuture<Void> processPageAsync(AzureGroup azureGroup,
                                                     DirectoryObjectCollectionResponse page, AtomicInteger membersCount) {
        if (page == null) {
            return CompletableFuture.completedFuture(null);
        }

        page.getValue().forEach(member -> {
            AzureGroupMembership azureGroupMembership = new AzureGroupMembership(azureGroup.getId(), member);
            if(azureGroupMembershipCache != null
                    && azureGroupMembershipCache.contains(azureGroupMembership.getId()))
            {
                log.info("Skipping message to Kafka, as userId: {} is already published as member of groupId: {}", member.getId(), azureGroup.getId());
            }
            else {
                azureGroupMembershipProducerService.publishAddedMembership(azureGroupMembership);
                azureGroupMembershipCache.add(azureGroupMembership.getId());
                membersCount.getAndIncrement();
                numMembers.getAndIncrement();
                log.debug("Produced message to Kafka where userId: {} is member of groupId: {}", member.getId(), azureGroup.getId());
            }

        });

        if (page.getOdataNextLink() != null) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return graphServiceClient.groups()
                            .byGroupId(azureGroup.getId())
                            .members()
                            .withUrl(page.getOdataNextLink())
                            .get();
                } catch (Exception e) {
                    log.error("Error fetching next member page for group {}: {}", azureGroup.getId(), e.getMessage());
                    return null;
                }
            }, executor).thenCompose(nextPage -> {
                if (nextPage != null) {
                    return processPageAsync(azureGroup, nextPage, membersCount);
                }
                return CompletableFuture.completedFuture(null);
            });
        }

        return CompletableFuture.completedFuture(null);
    }

    public boolean doesGroupExist(String resourceGroupId) throws Exception {
        // TODO: Attributes should not be hard-coded [FKS-210]
        String[] selectionCriteria = new String[]{String.format("id,displayName,description,%s", configGroup.getFintkontrollidattribute())};
        String filterCriteria = String.format(configGroup.getFintkontrollidattribute() + " eq '%s'", resourceGroupId);

        GroupCollectionResponse groupCollectionPage = graphServiceClient.groups()
                .get(requestConfiguration -> {
                    requestConfiguration.queryParameters.select = selectionCriteria;
                    requestConfiguration.queryParameters.filter = filterCriteria;
                });

        if (groupCollectionPage.getOdataNextLink() != null) {
            throw new Exception("doesGroupExist should only return a single group!");
        }

        for (Group group : groupCollectionPage.getValue()) {
            String attributeValue = group.getAdditionalData().get(configGroup.getFintkontrollidattribute()).toString();

            if (attributeValue != null && attributeValue.equals(resourceGroupId)) {
                return true;
            }
        }

        return false;
    }

    public void addGroupToAzure(ResourceGroup resourceGroup) {
        if (resourceGroup.getResourceName() != null &&
                !resourceGroup.getResourceName().trim().isEmpty() &&
                resourceGroup.getResourceType() != null &&
                !resourceGroup.getResourceType().trim().isEmpty() &&
                resourceGroup.getResourceType().length() > 3) {

            Group group = new MsGraphGroupMapper().toMsGraphGroup(resourceGroup, configGroup, config);
            String owner = "https://graph.microsoft.com/v1.0/directoryObjects/" + config.getCredentials().getEntobjectid();
            HashMap<String, Object> additionalData = new HashMap<>();
            LinkedList<String> ownersOdataBind = new LinkedList<>();
            ownersOdataBind.add(owner);
            additionalData.put("owners@odata.bind", ownersOdataBind);
            additionalData.put(configGroup.getFintkontrollidattribute(), resourceGroup.getId());
            group.setAdditionalData(additionalData);

            //TODO: Consider if uniqueName should be set upon creation of group
            //group.setUniqueName(resourceGroup.getId());

            CompletableFuture.runAsync(() -> {
                try {
                    Group createdGroup = graphServiceClient
                            .groups()
                            .post(group);

                    if (createdGroup != null) {
                        log.info("Added Group to Azure: {}", createdGroup.getDisplayName());
                        azureGroupProducerService.processGroup(new AzureGroup(createdGroup, configGroup));
                    }
                } catch (ApiException e) {
                    log.warn(e.getMessage());
                }

            }, executor).exceptionally(ex -> {
                log.error("Exception while adding group: {}", ex.getMessage(), ex);
                return null;
            });
        } else {
            log.error("addGroupToAzure cannot be completed as ResourceGroup with ID: {} does not have all required attributes set", resourceGroup.getId());
        }
    }

    public void deleteGroup(String resourceGroupId) {
        GroupCollectionResponse groupCollectionPage = null;
        String[] selectionCriteria = new String[]{String.format("id,%s", configGroup.getFintkontrollidattribute())};
        String filterCriteria = String.format(configGroup.getFintkontrollidattribute() + " eq '%s'", resourceGroupId);
        try {
            groupCollectionPage = graphServiceClient.groups()
                    .get(requestConfiguration -> {
                        requestConfiguration.queryParameters.select = selectionCriteria;
                        requestConfiguration.queryParameters.filter = filterCriteria;
                    });
        } catch (ApiException e) {
            log.error("Failed find the group in graph to be deleted using resourceGroupId {}: {}", resourceGroupId, e.getMessage());
        }

        while (groupCollectionPage != null) {
            for (Group group : groupCollectionPage.getValue()) {
                Object attributeValue = group.getAdditionalData().get(configGroup.getFintkontrollidattribute());

                if (attributeValue != null && attributeValue.equals(resourceGroupId)) {
                    try {
                        graphServiceClient.groups()
                                .byGroupId(group.getId())
                                .delete();
                        log.info("Group objectId {} and resourceGroupId {} deleted ", group.getId(), resourceGroupId);
                        return;
                    } catch (ApiException e) {
                        log.error("Failed to delete group with objectId: {} and resourceGroupId: {} \n Error message: {}", group.getId(), resourceGroupId, e.getMessage());
                        throw e;
                    }
                }
            }
        }
    }

    public void updateGroup(ResourceGroup resourceGroup) {

        Group group = new MsGraphGroupMapper().toMsGraphGroup(resourceGroup, configGroup, config);
        //group.setOwners(null);
        //group.setAdditionalData(null);

        //LinkedList<Option> requestOptions = new LinkedList<>();
        //requestOptions.add(new HeaderOption("Prefer", "create-if-missing"));

        Group groupResponse = graphServiceClient.groups()
                .byGroupId(resourceGroup.getIdentityProviderGroupObjectId())
                .patch(group);
        if (groupResponse != null) {
            log.info("Group with GroupObjectId '{}' successfully updated", resourceGroup.getIdentityProviderGroupObjectId());
        }
    }

    public void addGroupMembership(ResourceGroupMembership resourceGroupMembership, String resourceGroupMembershipKey) {
        if (resourceGroupMembership.getAzureUserRef() != null && resourceGroupMembership.getAzureGroupRef() != null) {

            DirectoryObject directoryObject = new DirectoryObject();
            directoryObject.setId(resourceGroupMembership.getAzureUserRef());
            ReferenceCreate referenceMember = new com.microsoft.graph.models.ReferenceCreate();
            referenceMember.setOdataId(String.format("https://graph.microsoft.com/v1.0/directoryObjects/%s", resourceGroupMembership.getAzureUserRef()));
            CompletableFuture.runAsync(() -> {
                try {
                    graphServiceClient.groups()
                            .byGroupId(resourceGroupMembership.getAzureGroupRef())
                            .members().ref().post(referenceMember);

                    log.info("UserId: {} added to GroupId: {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());

                    azureGroupMembershipProducerService.addMembership(new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject));

                    log.info("Produced message to Kafka on added UserId {} to GroupId {}",
                            resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());

                } catch (ApiException e) {
                    if (e.getResponseStatusCode() == 400) {
                        if (e.getMessage().contains("object references already exist")) {
                            azureGroupMembershipProducerService.addMembership(new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject));

                            log.info("Republished to Kafka, UserId {} already added to GroupId {}",
                                    resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                            return;
                        }
                        if (e.getMessage().contains("Request_ResourceNotFound")) {
                            log.warn("AzureGroupRef is not correct on user ObjectId {} and group ObjectId {}",
                                    resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                            return;
                        }

                        log.warn("Bad request: userRef: {} - groupRef: {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                        log.warn(e.getMessage());
                    }

                    if (e.getResponseStatusCode() == 404) {
                        log.warn("UserId: {} cannot be added to GroupId: {}. GroupId and/or UserId is not found in tenant",
                                resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                        return;
                    }
                    if (e.getResponseStatusCode() == 429) {
                        log.warn("Throttling limit. Error: {}", e.getMessage());
                    } else {
                        log.error("HTTP Error while updating group {}: {} \r",
                                resourceGroupMembership.getAzureGroupRef(), e.getMessage());
                    }
                }
            }, executor).exceptionally(ex -> {
                log.error("Exception while adding user to group: {}", ex.getMessage(), ex);
                return null;
            });
        }
    }

    public void deleteGroupMembership(String resourceGroupMembershipKey) {
        String[] splitString = resourceGroupMembershipKey.split("_");
        if (splitString.length != 2) {
            log.error("Key on kafka object {} not formatted correctly. NOT deleting membership from group", resourceGroupMembershipKey);
            return;
        }
        String groupId = splitString[0];
        String userId = splitString[1];

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Trying to remove UserId: {} from GroupId: {} in Graph", userId, groupId);

                graphServiceClient.groups()
                        .byGroupId(groupId)
                        .members()
                        .byDirectoryObjectId(userId)
                        .ref()
                        .delete();

                log.info("UserId: {} removed from GroupId: {}", userId, groupId);

                azureGroupMembershipProducerService.publishDeletedMembership(resourceGroupMembershipKey);
                resourceGroupMembershipCache.remove(resourceGroupMembershipKey);
                log.info("Produced message to Kafka on deleted UserId: {} from GroupId: {}", userId, groupId);

            } catch (ApiException e) {
                if (e.getResponseStatusCode() == 404) {
                    log.warn("User {} not found in group {}", userId, groupId);

                    azureGroupMembershipProducerService.publishDeletedMembership(resourceGroupMembershipKey);
                    resourceGroupMembershipCache.remove(resourceGroupMembershipKey);
                    log.warn("Produced message to Kafka on deleted UserId: {} from GroupId: {} as user not found in group", userId, groupId);

                } else {
                    log.error("HTTP Error while trying to remove user {} from group {}. Exception: {} \r{}",
                            userId, groupId, e.getResponseStatusCode(), e.getMessage());
                }
            } catch (Exception e) {
                log.error("Failed to process function deleteGroupMembership, Error: ", e);
            }
        },executor).exceptionally(ex -> {
            log.error("Exception while trying to remove user from group: {}", ex.getMessage(), ex);
            return null;
        });
    }
}
