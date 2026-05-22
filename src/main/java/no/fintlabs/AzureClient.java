package no.fintlabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.microsoft.graph.requests.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azure.*;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.ResourceGroupMembership;
import okhttp3.Request;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletionException;

@Component
@Slf4j
@RequiredArgsConstructor
public class AzureClient {
    protected final Config config;
    protected final ConfigGroup configGroup;
    protected final ConfigUser configUser;
    protected final GraphServiceClient<Request> graphService;
    private final AzureUserProducerService azureUserProducerService;
    private final AzureUserExternalProducerService azureUserExternalProducerService;
    private final AzureGroupProducerService azureGroupProducerService;
    private final AzureGroupMembershipProducerService azureGroupMembershipProducerService;
    private final ConcurrentHashMap<String, AzureUser> entraIdUserCache;
    private final ConcurrentHashMap<String, AzureUserExternal> entraIdExternalUserCache;
    private final ConcurrentHashMap<String, AzureGroup> azureGroupCache;
    private final ConcurrentHashMap<String, AzureGroupMembership> azureGroupMembershipCache;
    AtomicInteger publishedMembers;

    @Scheduled(cron = "${fint.kontroll.azure-ad-gateway.group-scheduler.clear-cache}")
    public void clearCaches() {
        entraIdUserCache.clear();
        entraIdExternalUserCache.clear();
        azureGroupCache.clear();
        azureGroupMembershipCache.clear();
        log.info("Caches for group, members and users has been reset to null due to scheduler. Next call will publish all users and groups from Entra ID to kafka");
    }

    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.fixed-delay-ms}"
    )
    private void pullAllUsers() {
        log.info("*** <<< Starting to pull users from Microsoft Entra >>> ***");
        long startTime = System.currentTimeMillis();
        try {
            this.pageThroughUsers(
                    graphService.users()
                            .buildRequest()
                            .select(String.join(",", configUser.AllAttributes()))
                            .filter("usertype eq 'member'")
                            .get()
            );
            long endTime = System.currentTimeMillis();
            long elapsedTimeInSeconds = (endTime - startTime) / 1000;
            long minutes = elapsedTimeInSeconds / 60;
            long seconds = elapsedTimeInSeconds % 60;

            log.info("*** <<< Finished pulling users from Microsoft Entra in {} minutes and {} seconds >>> *** ", minutes, seconds);
        } catch (ClientException ex) {
            log.error("pullAllUsers failed with message: {}", ex.getMessage().toString());
        }
    }

    private void pageThroughUsers(UserCollectionPage inPage) {
        AtomicInteger users = new AtomicInteger();
        AtomicInteger changedUsers = new AtomicInteger();
        AtomicInteger changedExtUsers = new AtomicInteger();

        UserCollectionPage page = inPage;
        do {
            for (User user : page.getCurrentPage()) {
                users.getAndIncrement();

                if (entraIdUserCache != null &&
                        entraIdUserCache.containsKey(user.id)) {
                    AzureUser entraIdUserObject = new AzureUser(user, configUser);
                    if (entraIdUserObject.equals(entraIdUserCache.get(user.id))) {
                        log.debug("User {} is unchanged. Skipping publishing to Kafka.", user.id);
                        continue;
                    }
                }

                String externalUserAttribute = AzureUser.getAttributeValue(user, configUser.getExternaluserattribute());
                if (configUser.getEnableExternalUsers() && externalUserAttribute != null
                        && externalUserAttribute.equalsIgnoreCase(configUser.getExternaluservalue())) {
                    AzureUserExternal entraUserExtObject = new AzureUserExternal(user, configUser);
                    if (entraIdExternalUserCache != null &&
                            entraIdExternalUserCache.containsKey(user.id) && entraUserExtObject.equals(entraIdExternalUserCache.get(user.id))) {
                        log.debug("External User {} is unchanged. Skipping publishing to Kafka.", user.id);
                    } else {
                        log.debug("Publishing external user to Kafka: {}", user.userPrincipalName);
                        azureUserExternalProducerService.publish(new AzureUserExternal(user, configUser));
                        log.info("Published external user to Kafka: {}", user.userPrincipalName);
                        log.debug("Updating cache for external user: {}", user.userPrincipalName);
                        changedExtUsers.getAndIncrement();
                        entraIdExternalUserCache.put(user.id, new AzureUserExternal(user, configUser));
                    }
                } else {
                    AzureUser azureuser = new AzureUser(user, configUser);
                    if ((azureuser.getEmployeeId() != null && !azureuser.getEmployeeId().isEmpty()) ||
                            (azureuser.getStudentId() != null && !azureuser.getStudentId().isEmpty())) {
                        log.debug("Publishing user to Kafka: {}", user.userPrincipalName);
                        azureUserProducerService.publish(azureuser);
                        log.info("Published user: {}", user.userPrincipalName);
                        log.debug("Updating cache for user: {}", user.id);
                        changedUsers.getAndIncrement();
                        entraIdUserCache.put(user.id, azureuser);
                    } else {
                        log.debug("UserId: {} does not contain required employeeId or studentId. Not published to kafka", user.id);
                    }
                }
//                if (AzureUser.getAttributeValue(user, configUser.getExternaluserattribute()) != null
//                        && (AzureUser.getAttributeValue(user, configUser.getExternaluserattribute()).equalsIgnoreCase(configUser.getExternaluservalue()))) {
//                    log.debug("Adding external user to Kafka, {}", user.userPrincipalName);
//                    azureUserExternalProducerService.publish(new AzureUserExternal(user, configUser));
//                } else {
//                    log.debug("Adding user to Kafka, {}", user.userPrincipalName);
//                    azureUserProducerService.publish(new AzureUser(user, configUser));
//                }
            }
            if (page.getNextPage() == null) {
                break;
            } else {
                page = page.getNextPage().buildRequest().get();
            }
        } while (page != null);
        log.info("*** <<< {} User objects detected in Microsoft Entra >>> ***", users);
        if (changedUsers.get() > 0) {
            log.info("*** <<< {} Entra users published to kafka as they where different from user cache >>> ***", changedUsers.get());
        }
        if (changedExtUsers.get() > 0) {
            log.info("*** <<< {} external users published to kafka as they where where different from external user cache >>> ***", changedExtUsers.get());
        }
    }

    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.delta-delay-ms}"
    )
    public void pullAllGroups() {
        log.info("*** <<< Fetching groups from Microsoft Entra >>> ***");
        long startTime = System.currentTimeMillis();
        publishedMembers = new AtomicInteger();

        try {
            CompletableFuture<GroupCollectionPage> initialPageFuture = graphService.groups()
                    .buildRequest()
                    .select(String.format("id,displayName,description,%s", configGroup.getFintkontrollidattribute()))
                    .getAsync();

            List<Group> allGroups = initialPageFuture
                    .thenCompose(this::fetchAllGroups)
                    .thenApply(groups -> {
                        long groupFetchEndTime = System.currentTimeMillis();
                        long elapsed = (groupFetchEndTime - startTime) / 1000;
                        log.info("*** <<< Done fetching all groups in {} minutes and {} seconds >>> ***", elapsed / 60, elapsed % 60);
                        return groups;
                    })
                    .join();

            log.info("*** <<< Fetching group memberships from Microsoft Entra >>> ***");
            long memberFetchStartTime = System.currentTimeMillis();
            fetchMembersForAllGroups(allGroups)
                    .thenAccept(groupCount -> {
                        long memberFetchEndTime = System.currentTimeMillis();
                        long elapsed = (memberFetchEndTime - memberFetchStartTime) / 1000;
                        log.info("*** <<< Done fetching all group memberships in {} minutes and {} seconds >>> ***", elapsed / 60, elapsed % 60);
                    })
                    .join();

        } catch (ClientException e) {
            log.error("Failed when trying to get groups. ", e);
        }

        if (publishedMembers.get() > 0) {
            log.info("*** <<< {} Entra group members published to kafka as they were not in membership cache >>> ***", publishedMembers.get());
        } else {
            log.info("*** <<< All Entra group members already in cache. No members published to kafka >>> ***");
        }
    }

    private CompletableFuture<List<Group>> fetchAllGroups(GroupCollectionPage initialPage) {
        List<Group> allGroups = new ArrayList<>();
        AtomicInteger groupCounter = new AtomicInteger(0);  // Counter for groups

        ConfigGroup.filterMode mode = configGroup.getFilterMode();
        if (mode == null) mode = ConfigGroup.filterMode.NONE;

        String prefix = configGroup.getPrefix();
        String suffix = configGroup.getSuffix();

        String filter = switch (mode) {
            case NONE   -> "no prefix or suffix filter";
            case PREFIX -> String.format("PREFIX startsWith \"%s\"", prefix);
            case SUFFIX -> String.format("SUFFIX endsWith \"%s\"", suffix);
            case BOTH   -> String.format("BOTH startsWith \"%s\" and endsWith \"%s\"", prefix, suffix);
        };

        return fetchAllGroupsRecursive(initialPage, allGroups, groupCounter).thenApply(v -> {
            if (groupCounter.get() > 0) {
                log.info("*** <<< Found {} groups using {} that was not in cache, and published to kafka >>> ***",
                        groupCounter.get(), filter);
            } else {
                log.info("*** <<< All groups already in cache. Not republishing to kafka >>> ***");
            }
            return allGroups;
        });

    }

    private CompletableFuture<Void> fetchAllGroupsRecursive(GroupCollectionPage currentPage, List<Group> allGroups, AtomicInteger groupCounter) {
        List<Group> currentPageGroups = currentPage.getCurrentPage().stream()
                //.filter(group -> group.displayName != null && group.displayName.endsWith(configGroup.getSuffix())&& (!group.additionalDataManager().isEmpty() && group.additionalDataManager().containsKey(configGroup.getFintkontrollidattribute())))
                .filter(group -> matchesNameFilter(group)
                        && group.additionalDataManager() != null
                        && group.additionalDataManager().containsKey(configGroup.getFintkontrollidattribute()))
                .peek(group -> {
                    AzureGroup newGroup = new AzureGroup(group, configGroup);
                    if(azureGroupCache != null
                            && azureGroupCache.containsKey(newGroup.getId())
                            && newGroup.equals(azureGroupCache.get(newGroup.getId()))) {
                        log.debug("{} groupID already published and in cache. Not republishing to kafka", newGroup.getId());
                    }
                    else
                    {
                        groupCounter.incrementAndGet();
                        azureGroupProducerService.publish(newGroup);  // Publish the group as soon as it is found
                        azureGroupCache.put(newGroup.getId(), newGroup);
                    }
                })
                .toList();
        allGroups.addAll(currentPageGroups);

        if (currentPage.getNextPage() != null) {
            return currentPage.getNextPage().buildRequest().getAsync()
                    .thenCompose(nextPage -> fetchAllGroupsRecursive(nextPage, allGroups, groupCounter));
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Integer> fetchMembersForAllGroups(List<Group> groups) {
        List<CompletableFuture<Void>> memberFutures = groups.stream()
                .map(group -> {
                    return graphService.groups(group.id).members()
                            .buildRequest()
                            .select("id")
                            .getAsync()
                            .thenCompose(memberPage -> {
                                AzureGroup newGroup = new AzureGroup(group, configGroup);
                                return pageThroughAzureGroupAsync(newGroup, memberPage);
                            })
                            .exceptionally(e -> {
                                log.error("Error fetching members for group {}: {}", group.id, e.getMessage());
                                return null;
                            });
                })
                .toList();

        return CompletableFuture.allOf(memberFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> groups.size());
    }


    private CompletableFuture<Void> pageThroughAzureGroupAsync(AzureGroup azureGroup, DirectoryObjectCollectionWithReferencesPage inPage) {
        AtomicInteger members = new AtomicInteger();



        return processPageAsync(azureGroup, inPage, members, publishedMembers)
                .thenRun(() -> log.debug("{} memberships detected in groupName {} with groupId {}",
                        members.get(), azureGroup.getDisplayName(), azureGroup.getId()));
    }

    private CompletableFuture<Void> processPageAsync(AzureGroup azureGroup, DirectoryObjectCollectionWithReferencesPage page, AtomicInteger members, AtomicInteger publishedMembers) {
        if (page == null) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> futures = page.getCurrentPage().stream()
                .map(member -> CompletableFuture.runAsync(() -> {
                    members.incrementAndGet();
                    AzureGroupMembership azureGroupMembership = new AzureGroupMembership(azureGroup.getId(), member);
                    if(azureGroupMembershipCache != null
                            && azureGroupMembershipCache.containsKey(azureGroupMembership.getId())
                            && azureGroupMembership.equals(azureGroupMembershipCache.get(azureGroupMembership.getId())))
                    {
                        log.debug("Skipping message to Kafka, as userId: {} is already published as member of groupId: {}", member.id, azureGroup.getId());
                    }
                    else {
                        azureGroupMembershipProducerService.publishAddedMembership(azureGroupMembership);
                        azureGroupMembershipCache.put(azureGroupMembership.getId(), azureGroupMembership);
                        publishedMembers.getAndIncrement();
                        log.debug("Produced message to Kafka where userId: {} is member of groupId: {}", member.id, azureGroup.getId());
                    }
                }))
                .toList();

        CompletableFuture<DirectoryObjectCollectionWithReferencesPage> nextPageFuture = (page.getNextPage() != null) ?
                page.getNextPage().buildRequest().getAsync() :
                CompletableFuture.completedFuture(null);

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenCompose(v -> nextPageFuture)
                .thenCompose(nextPage -> processPageAsync(azureGroup, nextPage, members, publishedMembers));
    }

    public boolean doesGroupExist(String resourceGroupId) {
        try {
            String selectionCriteria = String.format("id,%s", configGroup.getFintkontrollidattribute());

            GroupCollectionPage groupCollectionPage = graphService.groups()
                    .buildRequest()
                    .select(selectionCriteria)
                    .filter(String.format("%s eq '%s'", configGroup.getFintkontrollidattribute(), resourceGroupId))
                    .get();

            while (groupCollectionPage != null) {
                for (Group group : groupCollectionPage.getCurrentPage()) {
                    JsonElement attributeValue = group.additionalDataManager().get(configGroup.getFintkontrollidattribute());

                    if (attributeValue != null && resourceGroupId.equals(attributeValue.getAsString())) {
                        return true;
                    }
                }

                groupCollectionPage = (groupCollectionPage.getNextPage() == null)
                        ? null
                        : groupCollectionPage.getNextPage().buildRequest().get();
            }

            return false;

        } catch (Exception ex) {
            handleGraphApiError(ex);
            log.warn("Could not verify if group exists for resourceGroupId {}. Interpreting as EXISTS (safe default).",
                    resourceGroupId);

            return true;
        }
    }


    public void addGroupToAzure(ResourceGroup resourceGroup) {
        Group group = new MsGraphGroupMapper().toMsGraphGroup(resourceGroup, configGroup, config);

        //TODO: Remember to change from additionalDataManager to new function on Change of Graph to 6.*.* [FKS-883]
        String owner = "https://graph.microsoft.com/v1.0/directoryObjects/" + config.getEntobjectid();
        var owners = new JsonArray();
        owners.add(owner);
        group.additionalDataManager().put("owners@odata.bind", owners);

        //TODO: Consider if uniqueName chould be set upon creation of group
        //group.additionalDataManager().put("uniqueName", new JsonPrimitive(resourceGroup.getId()));

        graphService.groups()
                .buildRequest()
                .postAsync(group)
                .whenComplete((createdGroup, ex) -> {
                    if (ex == null) {
                        log.debug("Added Group to Azure: {}", group.displayName);
                        azureGroupProducerService.publish(new AzureGroup(createdGroup, configGroup));
                        log.info("Created group {} in Azure and published Group on kafka", createdGroup.displayName);
                    } else {
                        handleGraphApiError(ex);
                    }
                });
    }

    public void deleteGroup(String resourceGroupId) {
        try {
            GroupCollectionPage groupCollectionPage = graphService.groups()
                    .buildRequest()
                    .select(String.format("id, %s", configGroup.getFintkontrollidattribute()))
                    .filter(String.format(configGroup.getFintkontrollidattribute() + " eq '%s'", resourceGroupId))
                    .get();

            while (groupCollectionPage != null) {
                for (Group group : groupCollectionPage.getCurrentPage()) {
                    JsonElement attributeValue = group.additionalDataManager().get(configGroup.getFintkontrollidattribute());

                    if (attributeValue != null && attributeValue.getAsString().equals(resourceGroupId)) {
                        try {
                            graphService.groups(group.id)
                                    .buildRequest()
                                    .delete();
                            log.info("Group objectId {} and resourceGroupId {} deleted ", group.id, resourceGroupId);
                            return;
                        } catch (Exception e) {
                            log.error("Failed to delete group with objectId {} and resourceGroupId {}: {}", group.id, resourceGroupId, e.getMessage());
                            throw e;
                        }
                    }
                }

                groupCollectionPage = groupCollectionPage.getNextPage() != null
                        ? groupCollectionPage.getNextPage().buildRequest().get()
                        : null;
            }
        } catch (Exception e) {
            log.error("Failed to process deleteGroup for resourceGroupId {}: {}", resourceGroupId, e.getMessage());
            // Handle the exception as necessary, such as throwing it up the stack or logging it.
        }
    }

    public void deleteGroupAsync(String resourceGroupId) {
        CompletableFuture
                .supplyAsync(() -> graphService.groups()
                        .buildRequest()
                        .select(String.format("id, %s", configGroup.getFintkontrollidattribute()))
                        .filter(String.format("%s eq '%s'", configGroup.getFintkontrollidattribute(), resourceGroupId))
                        .get()
                )
                .thenCompose(page -> findAllMatchingGroupsAsync(page, resourceGroupId))
                .thenCompose(matches -> {
                    if (matches.isEmpty()) {
                        log.info("No group found for resourceGroupId {}", resourceGroupId);
                        return CompletableFuture.completedFuture(null);
                    }

                    if (matches.size() > 1) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Multiple groups found for resourceGroupId " + resourceGroupId +
                                        " (count=" + matches.size() + "). Aborting delete.")
                        );
                    }

                    Group toDelete = matches.getFirst();

                    return graphService.groups(toDelete.id)
                            .buildRequest()
                            .deleteAsync()
                            .thenAccept(ignored ->
                                    log.info("Group objectId {} with resourceId {} deleted", toDelete.id, resourceGroupId)
                            );
                })
                .whenComplete((ok, ex) -> {
                    if (ex != null) {
                        handleGraphApiError(ex);
                    }
                });
    }

    private CompletableFuture<java.util.List<Group>> findAllMatchingGroupsAsync(GroupCollectionPage page, String resourceGroupId) {
        java.util.List<Group> matches = new java.util.ArrayList<>();
        return findAllMatchingGroupsAsync(page, resourceGroupId, matches);
    }

    private CompletableFuture<java.util.List<Group>> findAllMatchingGroupsAsync(
            GroupCollectionPage page,
            String resourceGroupId,
            java.util.List<Group> acc
    ) {
        if (page == null) {
            return CompletableFuture.completedFuture(acc);
        }

        for (Group group : page.getCurrentPage()) {
            JsonElement attributeValue = group.additionalDataManager().get(configGroup.getFintkontrollidattribute());

            if (attributeValue != null && resourceGroupId.equals(attributeValue.getAsString())) {
                acc.add(group);
            }
        }

        if (page.getNextPage() == null) {
            return CompletableFuture.completedFuture(acc);
        }

        return CompletableFuture
                .supplyAsync(() -> page.getNextPage().buildRequest().get())
                .thenCompose(next -> findAllMatchingGroupsAsync(next, resourceGroupId, acc));
    }

    public void updateGroupAsync(ResourceGroup resourceGroup) {
        Group group = new MsGraphGroupMapper().toMsGraphGroup(resourceGroup, configGroup, config);
        group.owners = null;
        group.additionalDataManager().clear();

        String objectId = resourceGroup.getIdentityProviderGroupObjectId();

        graphService.groups(objectId)
                .buildRequest()
                .patchAsync(group)
                .whenComplete((updatedGroup, ex) -> {
                    if (ex == null) {
                        log.info("Group with GroupObjectId '{}' successfully updated", objectId);
                    } else {
                        handleGraphApiError(ex);
                    }
                });
    }


    public void addGroupMembership(ResourceGroupMembership resourceGroupMembership, String resourceGroupMembershipKey) {
        if (resourceGroupMembership.getAzureUserRef() == null ||
                resourceGroupMembership.getAzureGroupRef() == null) {
            log.warn("Skipping addGroupMembership, missing user or group ref. userRef={}, groupRef={}",
                    resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
            return;
        }

        DirectoryObject directoryObject = new DirectoryObject();
        directoryObject.id = resourceGroupMembership.getAzureUserRef();

        try {
            DirectoryObjectCollectionReferenceRequestBuilder references = graphService.groups(resourceGroupMembership.getAzureGroupRef()).members().references();

            if (references == null) {
                log.error("Member references is null for group {}", resourceGroupMembership.getAzureGroupRef());
                return;
            }

            if (azureGroupMembershipCache.containsKey(resourceGroupMembershipKey)) {
                log.info("Membership {} found in cache, but will still verify/add in EntraID", resourceGroupMembershipKey);
            }

            references.buildRequest()
                    .postAsync(directoryObject)
                    .whenComplete((acceptedMember, throwable) -> {
                        if (throwable != null) {
                            Throwable cause = (throwable instanceof CompletionException && throwable.getCause() != null)
                                    ? throwable.getCause()
                                    : throwable;

                            if (cause instanceof GraphServiceException gse && gse.getResponseCode() == 400) {
                                String msg = (gse.getError() != null && gse.getError().error != null)
                                        ? gse.getError().error.message
                                        : null;

                                if (msg != null && msg.contains("object references already exist")) {
                                    try {
                                        azureGroupMembershipProducerService.publishAddedMembership(
                                                new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject)
                                        );
                                        azureGroupMembershipCache.put(resourceGroupMembershipKey,
                                                new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject));
                                        log.info("Already member in EntraID. Published Added to Kafka. userId={}, groupId={}",
                                                resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                                    } catch (Exception kafkaEx) {
                                        log.error("Failed to publish Added to Kafka (already exists case). key={}",
                                                resourceGroupMembershipKey, kafkaEx);
                                    }
                                    return;
                                }
                            }

                            handleGraphApiError(throwable);
                            return;
                        }
                        try {
                            azureGroupMembershipProducerService.publishAddedMembership(
                                    new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject)
                            );
                            azureGroupMembershipCache.put(resourceGroupMembershipKey,
                                    new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject));
                            log.info("Added in EntraID. Published Added to Kafka. userId={}, groupId={}",
                                    resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                        } catch (Exception kafkaEx) {
                            log.error("User added to EntraID, but failed to publish Added to Kafka. key={}",
                                    resourceGroupMembershipKey, kafkaEx);
                        }
                    });

        } catch (GraphServiceException e) {
            handleGraphApiError(e);
        } catch (Exception e) {
            log.error("Failed to process addGroupMembership for resourceGroupId {}: {}", resourceGroupMembership.getAzureGroupRef(), e.getMessage(), e);
        }
    }

    public void deleteGroupMembership(String resourceGroupMembershipKey) {
        String[] splitString = resourceGroupMembershipKey.split("_");
        if (splitString.length != 2) {
            log.error("Key on kafka object {} not formatted correctly. NOT deleting membership from group",
                    resourceGroupMembershipKey);
            return;
        }
        String group = splitString[0];
        String user = splitString[1];

        try {
            log.debug("Trying to remove UserId: {} from GroupId: {} in Graph", user, group);

            DirectoryObjectReferenceRequestBuilder reference = graphService.groups(group)
                    .members(user)
                    .reference();

            if (reference == null) {
                log.error("Member reference is null for group {}", group);
                return;
            }

            reference.buildRequest()
                    .deleteAsync()
                    .whenComplete((ignored, throwable) -> {
                        if (throwable != null) {
                            Throwable cause = (throwable instanceof CompletionException && throwable.getCause() != null)
                                    ? throwable.getCause()
                                    : throwable;

                            if (cause instanceof GraphServiceException gse && gse.getResponseCode() == 404) {
                                log.warn("User {} not found in group {} in Entra when trying to delete membership. Publishing delete to Kafka", user, group);
                                if (azureGroupMembershipCache.containsKey(resourceGroupMembershipKey)) {
                                    azureGroupMembershipCache.remove(resourceGroupMembershipKey);
                                }
                                try {
                                    azureGroupMembershipProducerService.publishDeletedMembership(resourceGroupMembershipKey);
                                    log.debug("Produced message to kafka on deleted UserId: {} from GroupId: {}",
                                            user, group);
                                } catch (Exception kafkaEx) {
                                    log.error("Failed to publish deleted membership (404 case) {} to Kafka",
                                            resourceGroupMembershipKey, kafkaEx);
                                }
                            } else {
                                handleGraphApiError(throwable);
                            }
                            return;
                        }

                        log.info("UserId: {} removed from GroupId: {}. Publishing delete to kafka", user, group);

                        try {
                            if (azureGroupMembershipCache.containsKey(resourceGroupMembershipKey)) {
                                azureGroupMembershipCache.remove(resourceGroupMembershipKey);
                            }
                            azureGroupMembershipProducerService.publishDeletedMembership(resourceGroupMembershipKey);
                            log.debug("Produced message to kafka on deleted UserId: {} from GroupId: {}", user, group);
                        } catch (Exception kafkaEx) {
                            log.error("User removed from EntraID, but failed to publish delete to Kafka. userId={}, groupId={}",
                                    user, group, kafkaEx);
                        }
                    });

        } catch (GraphServiceException e) {
            if (e.getResponseCode() == 404) {
                log.warn("User {} not found in group {}", user, group);
                if (azureGroupMembershipCache.containsKey(resourceGroupMembershipKey)) {
                    azureGroupMembershipCache.remove(resourceGroupMembershipKey);
                }
                try {
                    azureGroupMembershipProducerService.publishDeletedMembership(resourceGroupMembershipKey);
                    log.debug("Produced message to kafka on deleted UserId: {} from GroupId: {} (sync 404 case)",
                            user, group);
                } catch (Exception kafkaEx) {
                    log.error("Failed to publish deleted membership {} to Kafka",
                            resourceGroupMembershipKey, kafkaEx.getMessage());
                }
            } else {
                handleGraphApiError(e);
            }
        } catch (Exception e) {
            log.error("Failed to process function deleteGroupMembership for key {} (user={}, group={}). Error:",
                    resourceGroupMembershipKey, user, group, e);
        }
    }

    private boolean matchesNameFilter(Group group) {
        if (group == null || group.displayName == null) return false;

        var mode = configGroup.getFilterMode();
        if (mode == null) mode = ConfigGroup.filterMode.NONE;

        String name = group.displayName.toLowerCase();

        String prefix = configGroup.getPrefix();
        String suffix = configGroup.getSuffix();
        String p = (prefix == null) ? "" : prefix.trim().toLowerCase();
        String s = (suffix == null) ? "" : suffix.trim().toLowerCase();

        return switch (mode) {
            case NONE   -> true;
            case PREFIX -> !p.isEmpty() && name.startsWith(p);
            case SUFFIX -> !s.isEmpty() && name.endsWith(s);
            case BOTH   -> !p.isEmpty() && !s.isEmpty() && name.startsWith(p) && name.endsWith(s);
        };
    }


    private void handleGraphApiError(Throwable ex) {
        if (ex instanceof CompletionException) {
            Throwable cause = ex.getCause();
            if (cause instanceof GraphServiceException gse) {
                int statusCode = gse.getResponseCode();
                String errorMessage = (gse.getError() != null && gse.getError().error != null)
                        ? gse.getError().error.message
                        : "No error message";
                switch (statusCode) {
//                    case 204:
//                        log.info("No content response received.");
//                        break;
                    case 400:
                        log.error("Group not created or updated. Failed with error code {}. {}", statusCode, errorMessage);
                        break;
                    case 401:
                        log.error("Unauthorized. Check your authentication credentials");
                        break;
                    case 403:
                        log.error("Forbidden. You do not have permission to perform this action");
                        break;
                    case 404:
                        log.error("Error code: {}. {}",statusCode, errorMessage);
                        break;
                    case 429:
                        log.warn("Throttling limit. Error: {}", errorMessage);
                        break;
                    case 500:
                        log.error("Internal server error. Try again later");
                        break;
                    default:
                        log.error("Unexpected error: {}", gse.getMessage());
                }
            } else {
                log.error("An unexpected error occurred: {}", cause.getMessage());
            }
        } else {
            log.error("An unexpected error occurred: {}", ex.getMessage());
        }
    }
}