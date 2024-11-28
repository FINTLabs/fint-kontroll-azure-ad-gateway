package no.fintlabs;

import com.microsoft.graph.core.tasks.PageIterator;
import com.microsoft.graph.groups.delta.DeltaGetResponse;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.serialization.UntypedArray;
import com.microsoft.kiota.serialization.UntypedObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.azure.*;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.ResourceGroupMembership;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@Log4j2
@RequiredArgsConstructor
public class AzureClient {
    protected final Config config;
    protected final ConfigGroup configGroup;
    protected final ConfigUser configUser;
    protected final GraphServiceClient graphServiceClient;
    private String deltaLinkCache;
    private final AzureUserProducerService azureUserProducerService;
    private final AzureUserExternalProducerService azureUserExternalProducerService;
    private final AzureGroupProducerService azureGroupProducerService;
    private final AzureGroupMembershipProducerService azureGroupMembershipProducerService;

    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.fixed-delay-ms}"
    )
    private void pullAllUsers() {
        log.info("*** <<< Starting to pull users from Microsoft Entra >>> ***");
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

            long endTime = System.currentTimeMillis();
            long elapsedTimeInSeconds = (endTime - startTime) / 1000;
            long minutes = elapsedTimeInSeconds / 60;
            long seconds = elapsedTimeInSeconds % 60;

            log.info("*** <<< Finished pulling users from Microsoft Entra in {} minutes and {} seconds >>> *** ", minutes, seconds);

        } catch (ApiException | ReflectiveOperationException ex) {
            log.error("pullAllUsers failed with message: {}", ex.getMessage());
        }
    }

    private void pageThroughUsers(UserCollectionResponse userPage) throws ReflectiveOperationException {
        AtomicInteger users = new AtomicInteger();

        //List<User> allUsers = new LinkedList<>();
        PageIterator<User, UserCollectionResponse> pageIterator = new PageIterator.Builder<User, UserCollectionResponse>()
                .client(graphServiceClient)
                .collectionPage(userPage)
                .collectionPageFactory(UserCollectionResponse::createFromDiscriminatorValue)
                .processPageItemCallback(user -> {
                    users.getAndIncrement();
                    //allUsers.add(user);
                    if (AzureUser.getAttributeValue(user, configUser.getExternaluserattribute()) != null
                            && (AzureUser.getAttributeValue(user, configUser.getExternaluserattribute()).equalsIgnoreCase(configUser.getExternaluservalue()))) {
                        log.debug("Adding external user to Kafka, {}", user.getUserPrincipalName());
                        azureUserExternalProducerService.publish(new AzureUserExternal(user, configUser));
                    } else {
                        log.debug("Adding user to Kafka, {}", user.getUserPrincipalName());
                        azureUserProducerService.publish(new AzureUser(user, configUser));
                    }
                    return true;
                }).build();
        pageIterator.iterate();
    }

//    TODO: Implement Delta. Scheduler is as for now disabled

    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.delta-pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.delta-pull.delta-delay-ms}"
    )
    public void pullAllGroupsDelta() {
        log.info("*** <<< Fetching groups and members using delta call from Microsoft Entra >>> ***");
        String[] selectionCriteria = new String[]{String.format("id,displayName,description,members,%s", configGroup.getFintkontrollidattribute())};

        try {
            if (deltaLinkCache != null) {
                String deltaUrl = deltaLinkCache;
                DeltaGetResponse groupPage = graphServiceClient.groups().delta().withUrl(deltaUrl)
                        .get(requestConfiguration -> {
                            requestConfiguration.queryParameters.select = selectionCriteria;
                            requestConfiguration.queryParameters.top = configGroup.getGrouppagingsize();
                        });
                pageThroughGroupsDelta(groupPage);
            } else {
                DeltaGetResponse groupPage = graphServiceClient.groups().delta()
                        .get(requestConfiguration -> {
                            requestConfiguration.queryParameters.select = selectionCriteria;
                            requestConfiguration.queryParameters.top = configGroup.getGrouppagingsize();
                        });
                pageThroughGroupsDelta(groupPage);
            }

        } catch (ApiException | ReflectiveOperationException e) {
            log.error("Failed when trying to get groups. ", e);
        }
    }

    private List<AzureGroup> pageThroughGroupsDelta(DeltaGetResponse groupPage) throws ReflectiveOperationException {
        List<AzureGroup> allGroups = new ArrayList<>();
        AtomicInteger groupCounter = new AtomicInteger(0);
        Set<String> processedGroupIds = new HashSet<>();
        long startTime = System.currentTimeMillis();

        while (true) {
            // Process the current page and check for duplicates
            deltaPageIterator(groupPage, groupCounter, allGroups, processedGroupIds);

            // If @odataNextLink is present, fetch the next group page
            if (groupPage.getOdataNextLink() != null) {
                groupPage = graphServiceClient.groups().delta().withUrl(groupPage.getOdataNextLink()).get();
            } else {
                break;
            }
        }
        long endTime = System.currentTimeMillis();
        long elapsedTimeInSeconds = (endTime - startTime) / 1000;
        long minutes = elapsedTimeInSeconds / 60;
        long seconds = elapsedTimeInSeconds % 60;
        if (groupPage.getOdataDeltaLink() == null) {
            log.error("Logic error: Last page doesn't contain ODataDeltaLink");
            throw new ReflectiveOperationException("Logic error: Last page doesn't contain ODataDeltaLink");
        }
        if(deltaLinkCache == null)
        {
            log.info("*** <<< Initial Delta run on Groups completed >>> ***");
            log.info("*** <<< Found {} groups with suffix \"{}\", in {} minutes and {} seconds >>> ***",
                    groupCounter.get(),
                    configGroup.getSuffix(),
                    minutes,
                    seconds);
        }
        else {
            log.info("*** <<< Found {} changed groups with suffix \"{}\", in {} minutes and {} seconds since last Delta run >>> ***",
                    groupCounter.get(),
                    configGroup.getSuffix(),
                    minutes,
                    seconds);
        }
        log.debug("Setting delta link to Cache");
        deltaLinkCache = groupPage.getOdataDeltaLink();
        return allGroups;
    }

    private void deltaPageIterator(DeltaGetResponse groupPage, AtomicInteger groupCounter, List<AzureGroup> allGroups, Set<String> processedGroupIds) throws ReflectiveOperationException {
        PageIterator<Group, DeltaGetResponse> pageIterator = new PageIterator.Builder<Group, DeltaGetResponse>()
                .client(graphServiceClient)
                .collectionPage(groupPage)
                .collectionPageFactory(DeltaGetResponse::createFromDiscriminatorValue)
                .processPageItemCallback(group -> {
                    // Ensure the group has not been processed yet
                    if (group.getDisplayName() != null && group.getDisplayName().endsWith(configGroup.getSuffix())
                            && (!group.getAdditionalData().isEmpty()
                            && group.getAdditionalData().containsKey(configGroup.getFintkontrollidattribute()))
                            && processedGroupIds.add(group.getId())) { // Only process if it's new

                        groupCounter.getAndIncrement();

                        // Create and process the AzureGroup object
                        AzureGroup newGroup = new AzureGroup(group, configGroup);

                        // Publish the group
                        azureGroupProducerService.publish(newGroup);

                        // Add to allGroups collection
                        allGroups.add(newGroup);
                        processMembersDelta(group);
                    }
                    return true;
                }).build();

        // Iterates through all pages and collects groups
        pageIterator.iterate();
    }

    private void processMembersDelta(Group group) {
        Map<String, Object> additionalData = group.getAdditionalData();
        if (additionalData == null) {
            return;
        }
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
                    azureGroupMembershipProducerService.publishDeletedMembership(kafkaKey);
                    log.debug("Produced message to Kafka on removed user with ObjectID: {} from group: {}", memberId, group.getId());
                    log.info("UserId: {} is removed as member from GroupId: {}", memberId, group.getId());
                    continue;
                }

                azureGroupMembershipProducerService.publishAddedMembership(new AzureGroupMembership(memberId,group.getId(),kafkaKey));
                log.debug("Produced message to Kafka where userId: {} is member of groupId: {}", memberId, group.getId());
                log.info("UserId: {} is member of GroupId: {}", memberId, group.getId());
            }

        } catch (ClassCastException e) {
            log.error("Failed to process members@delta. Error: {}", e.getMessage());
        }
    }

//  TODO: Consider if this should be deactivated if delta is implemented
    /*@Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.delta-delay-ms}"
    )*/
    public void pullAllGroupsAsync() {
        log.info("*** <<< Fetching groups from Microsoft Entra >>> ***");
        long startTime = System.currentTimeMillis();
        String[] selectionCriteria = new String[]{String.format("id,displayName,description,%s", configGroup.getFintkontrollidattribute())};
        // Define an executor for asynchronous tasks
        ExecutorService executor = Executors.newFixedThreadPool(4);  // Can adjust thread pool size if needed

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
            log.info("*** <<< {} groups processed in {} minutes and {} seconds. Continuing processing memberships >>> ***", allGroups.size(), minutes, seconds);
            fetchAndPublishMembersForAllGroupsAsync(allGroups);

        }).exceptionally(ex -> {
            log.error("An error occurred while fetching groups: {}", ex.getMessage());
            return null;
        }).thenRun(() -> {
            long endTime = System.currentTimeMillis();
            long elapsedTimeInSeconds = (endTime - startTime) / 1000;
            long minutes = elapsedTimeInSeconds / 60;
            long seconds = elapsedTimeInSeconds % 60;
            log.info("*** <<< Done processing groups and memberships in {} minutes and {} seconds >>> ***", minutes, seconds);
        }).join();
    }

    private List<AzureGroup> pageThroughGroups(GroupCollectionResponse groupPage) throws ReflectiveOperationException {
        List<AzureGroup> allGroups = new ArrayList<>();
        AtomicInteger groupCounter = new AtomicInteger(0);

        PageIterator<Group, GroupCollectionResponse> pageIterator = new PageIterator.Builder<Group, GroupCollectionResponse>()
                .client(graphServiceClient)
                .collectionPage(groupPage)
                .collectionPageFactory(GroupCollectionResponse::createFromDiscriminatorValue)
                .processPageItemCallback(group -> {
                    if (group.getDisplayName() != null && group.getDisplayName().endsWith(configGroup.getSuffix())
                            && (!group.getAdditionalData().isEmpty() && group.getAdditionalData().containsKey(configGroup.getFintkontrollidattribute()))) {
                        groupCounter.getAndIncrement();
                        AzureGroup newGroup = new AzureGroup(group, configGroup);
                        azureGroupProducerService.publish(newGroup);
                        allGroups.add(newGroup);
                    }
                    return true;
                }).build();

        pageIterator.iterate();

        log.info("*** <<< Found {} groups with suffix \"{}\" >>> ***", groupCounter.get(), configGroup.getSuffix());

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
                }).thenCompose(memberPage -> {
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
        AtomicInteger membersCount = new AtomicInteger(0);

        return processPageAsync(azureGroup, inPage, membersCount)
                .thenRun(() -> log.debug("{} memberships detected in groupName \"{}\" with groupId {}",
                        membersCount.get(), azureGroup.getDisplayName(), azureGroup.getId()));
    }

    private CompletableFuture<Void> processPageAsync(AzureGroup azureGroup,
                                                     DirectoryObjectCollectionResponse page, AtomicInteger membersCount) {
        if (page == null) {
            return CompletableFuture.completedFuture(null);  // If there's no page, complete immediately
        }

        page.getValue().forEach(member -> {
            membersCount.incrementAndGet();
            log.debug("Processing member with UserID: {}", member.getId());
            azureGroupMembershipProducerService.publishAddedMembership(new AzureGroupMembership(azureGroup.getId(), member));
            log.info("Produced message to Kafka where userId: {} is member of groupId: {}", member.getId(), azureGroup.getId());
        });

        if (page.getOdataNextLink() != null) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return graphServiceClient.groups()
                            .byGroupId(azureGroup.getId())
                            .members()
                            .withUrl(page.getOdataNextLink())  // Follow the next link
                            .get();
                } catch (Exception e) {
                    log.error("Error fetching next member page for group {}: {}", azureGroup.getId(), e.getMessage());
                    return null;
                }
            }).thenCompose(nextPage -> {
                if (nextPage != null) {
                    return processPageAsync(azureGroup, nextPage, membersCount);  // Process the next page asynchronously
                }
                return CompletableFuture.completedFuture(null);
            });
        }

        return CompletableFuture.completedFuture(null);  // If there's no next page, return completed future
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
                return true; // Group with the specified ResourceID found
            }
        }

        return false; // Group with resourceID not found
    }

    public void addGroupToAzure(ResourceGroup resourceGroup) {
        Group group = new MsGraphGroupMapper().toMsGraphGroup(resourceGroup, configGroup, config);

        //TODO: Remember to change from additionalDataManager to new function on Change of Graph to 6.*.* [FKS-883]
        String owner = "https://graph.microsoft.com/v1.0/directoryObjects/" + config.getEntobjectid();
        HashMap<String, Object> additionalData = new HashMap<String, Object>();
        LinkedList<String> ownersOdataBind = new LinkedList<String>();
        ownersOdataBind.add(owner);
        additionalData.put("owners@odata.bind", ownersOdataBind);
        group.setAdditionalData(additionalData);

        //TODO: Consider if uniqueName chould be set upon creation of group
        //group.setUniqueName(resourceGroup.getId());

        CompletableFuture.runAsync(() -> {
            try {
                Group createdGroup = graphServiceClient
                        .groups()
                        .post(group);

                if (createdGroup != null) {
                    log.info("Added Group to Azure: {}", createdGroup.getDisplayName());
                    azureGroupProducerService.publish(new AzureGroup(createdGroup, configGroup));
                }
            } catch (ApiException e) {

                // Handling 400 Bad Request error
                log.warn(e.getMessage());
            }

        }).exceptionally(ex -> {
            log.error("Exception while adding group: {}", ex.getMessage(), ex);
            return null; // Exceptionally should return a value
        });
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
            // Handle the exception as necessary, such as throwing it up the stack or logging it.
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
        group.setOwners(null);
        group.setAdditionalData(null);

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
                            .members().ref().post(referenceMember); // Posting referenceMember asynchronously

                    log.info("UserId: {} added to GroupId: {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());

                    // Publishing the added membership to Kafka
                    azureGroupMembershipProducerService.publishAddedMembership(
                            new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject));

                    log.debug("Produced message to Kafka on added UserId {} to GroupId {}",
                            resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());

                } catch (ApiException e) {
                    // Handling HTTP response exception here
                    if (e.getResponseStatusCode() == 400) {
                        if (e.getMessage().contains("object references already exist")) {
                            azureGroupMembershipProducerService.publishAddedMembership(
                                    new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject));

                            log.debug("Republished to Kafka, UserId {} already added to GroupId {}",
                                    resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                            return;
                        }
                        if (e.getMessage().contains("Request_ResourceNotFound")) {
                            log.warn("AzureGroupRef is not correct on user ObjectId {} and group ObjectId {}",
                                    resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                            return;
                        }

                        // Handling 400 Bad Request error
                        log.warn("Bad request: ", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                        log.warn(e.getMessage());
                    }

                    if (e.getResponseStatusCode() == 429) {
                        log.warn("Throttling limit. Error: {}", e.getMessage());
                    } else {
                        // Handling other HTTP errors
                        log.error("HTTP Error while updating group {}: {} \r",
                                resourceGroupMembership.getAzureGroupRef(), e.getMessage());
                    }
                }
            }).exceptionally(ex -> {
                log.error("Exception while adding user to group: {}", ex.getMessage(), ex);
                return null; // Exceptionally should return a value
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
                log.debug("Trying to remove UserId: {} from GroupId: {} in Graph", userId, groupId);

                // Asynchronously delete the user from the group
                graphServiceClient.groups()
                        .byGroupId(groupId)
                        .members()
                        .byDirectoryObjectId(userId)
                        .ref()
                        .delete();

                log.info("UserId: {} removed from GroupId: {}", userId, groupId);

                // Publish to Kafka after removal
                azureGroupMembershipProducerService.publishDeletedMembership(resourceGroupMembershipKey);
                log.debug("Produced message to Kafka on deleted UserId: {} from GroupId: {}", userId, groupId);

            } catch (ApiException e) {
                if (e.getResponseStatusCode() == 404) {
                    log.warn("User {} not found in group {}", userId, groupId);

                    // Publish to Kafka if the user is not found
                    azureGroupMembershipProducerService.publishDeletedMembership(resourceGroupMembershipKey);
                    log.debug("Produced message to Kafka on deleted UserId: {} from GroupId: {}", userId, groupId);

                } else {
                    log.error("HTTP Error while trying to remove user {} from group {}. Exception: {} \r{}",
                            userId, groupId, e.getResponseStatusCode(), e.getMessage());
                }
            } catch (Exception e) {
                log.error("Failed to process function deleteGroupMembership, Error: ", e);
            }
        }).exceptionally(ex -> {
            // Handle any exceptions that might occur
            log.error("Exception while trying to remove user from group: {}", ex.getMessage(), ex);
            return null; // exceptionally must return a value
        });
    }
}
