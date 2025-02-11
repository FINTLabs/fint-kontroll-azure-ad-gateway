package no.fintlabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.graph.requests.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.azure.*;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.ResourceGroupMembership;
import okhttp3.Request;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletionException;

@Component
@Log4j2
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
    private final FintCache<String, AzureUser> entraIdUserCache;
    private final FintCache<String, AzureUserExternal> entraIdExternalUserCache;
    //private final FintCache<String, Optional> resourceGroupMembershipCache;
    private final FintCache<String, AzureGroup> azureGroupCache;
    private final FintCache<String, AzureGroupMembership> azureGroupMembershipCache;
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
                        return;
                    }
                }

                String externalUserAttribute = AzureUser.getAttributeValue(user, configUser.getExternaluserattribute());
                if (externalUserAttribute != null
                        && externalUserAttribute.equalsIgnoreCase(configUser.getExternaluservalue())) {
                    AzureUserExternal entraUserExtObject = new AzureUserExternal(user, configUser);
                    if (entraIdExternalUserCache != null &&
                            entraIdExternalUserCache.containsKey(user.id) && entraUserExtObject.equals(entraIdExternalUserCache.get(user.id))) {
                        log.debug("External User {} is unchanged. Skipping publishing to Kafka.", user.id);
                        return;
                    } else {
                        log.debug("Publishing external user to Kafka: {}", user.userPrincipalName);
                        azureUserExternalProducerService.publish(new AzureUserExternal(user, configUser));
                        changedExtUsers.getAndIncrement();
                        entraIdExternalUserCache.put(user.id, new AzureUserExternal(user, configUser));
                    }
                } else {
                    AzureUser azureuser = new AzureUser(user, configUser);
                    if ((azureuser.getEmployeeId() != null && !azureuser.getEmployeeId().isEmpty()) ||
                            (azureuser.getStudentId() != null && !azureuser.getStudentId().isEmpty())) {
                        log.debug("Publishing user to Kafka: {}", user.userPrincipalName);
                        azureUserProducerService.publish(azureuser);
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

            initialPageFuture.thenCompose(this::fetchAllGroups)
                    .thenCompose(groups -> {
                        long groupFetchEndTime = System.currentTimeMillis();
                        long groupFetchElapsedTimeInSeconds = (groupFetchEndTime - startTime) / 1000;
                        long groupFetchMinutes = groupFetchElapsedTimeInSeconds / 60;
                        long groupFetchSeconds = groupFetchElapsedTimeInSeconds % 60;
                        log.info("*** <<< Done fetching all groups from Microsoft Entra ID in {} minutes and {} seconds >>> ***", groupFetchMinutes, groupFetchSeconds);

                        log.info("*** <<< Fetching group memberships from Microsoft Entra >>> ***");
                        long memberFetchStartTime = System.currentTimeMillis();
                        return fetchMembersForAllGroups(groups).thenApply(groupCount -> {
                            long memberFetchEndTime = System.currentTimeMillis();
                            long memberFetchElapsedTimeInSeconds = (memberFetchEndTime - memberFetchStartTime) / 1000;
                            long memberFetchMinutes = memberFetchElapsedTimeInSeconds / 60;
                            long memberFetchSeconds = memberFetchElapsedTimeInSeconds % 60;


                            log.info("*** <<< Done fetching all group memberships from Microsoft Entra ID in {} minutes and {} seconds >>> ***", memberFetchMinutes, memberFetchSeconds);
                            return groupCount;
                        });
                    })
                    //.thenAccept(groupCount -> log.info("{} Group objects fetched from Microsoft Entra ID with suffix {}", groupCount, configGroup.getSuffix()))
                    .join();  // Wait for completion

        } catch (ClientException e) {
            log.error("Failed when trying to get groups. ", e);
        }
        if (publishedMembers.get() > 0) {
            log.info("*** <<< {} Entra group members published to kafka as they were not in membership cache >>> ***", publishedMembers.get());
        } else {
            log.info("*** <<< All entra group members already in cache. No members published to kafka >>> ***");
        }
    }


    private CompletableFuture<List<Group>> fetchAllGroups(GroupCollectionPage initialPage) {
        List<Group> allGroups = new ArrayList<>();
        AtomicInteger groupCounter = new AtomicInteger(0);  // Counter for groups

        return fetchAllGroupsRecursive(initialPage, allGroups, groupCounter).thenApply(v -> {
            if(groupCounter.get() > 0) {
                log.info("*** <<< Found {} groups with suffix \"{}\" not in cache, that were published to kafka >>> ***", groupCounter.get(), configGroup.getSuffix());
            }
            else {
                log.info("*** <<< All groups already in cache. Not republishing to kafka >>> ***");
            }
            return allGroups;
        });
    }

    private CompletableFuture<Void> fetchAllGroupsRecursive(GroupCollectionPage currentPage, List<Group> allGroups, AtomicInteger groupCounter) {
        List<Group> currentPageGroups = currentPage.getCurrentPage().stream()
                .filter(group -> group.displayName != null && group.displayName.endsWith(configGroup.getSuffix())&& (!group.additionalDataManager().isEmpty() && group.additionalDataManager().containsKey(configGroup.getFintkontrollidattribute())))
                .peek(group -> {
                    AzureGroup newGroup = new AzureGroup(group, configGroup);
                    if(azureGroupCache != null
                            && azureGroupCache.containsKey(newGroup.getId())
                            && newGroup.equals(azureGroupCache.get(newGroup.getId()))) {
                        log.debug("{} groupID allready published and in cache. Not replublished to kafka", newGroup.getId());
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
                        log.debug("Skipping message to Kafka, as userId: {} is allready published as member of groupId: {}", member.id, azureGroup.getId());
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
//    public void pullAllGroups() {
//        log.info("*** <<< Fetching groups from Microsoft Entra >>> ***");
//        long startTime = System.currentTimeMillis();
//
//        try {
//            CompletableFuture<GroupCollectionPage> initialPageFuture = graphService.groups()
//                    .buildRequest()
//                    .select(String.format("id,displayName,description,%s", configGroup.getFintkontrollidattribute()))
//                    .getAsync();
//
//            CompletableFuture<Integer> resultFuture = pageThroughGroups(initialPageFuture);
//            resultFuture.thenAccept(groups -> {
//                long endTime = System.currentTimeMillis();
//                long elapsedTimeInSeconds = (endTime - startTime) / 1000;
//                long minutes = elapsedTimeInSeconds / 60;
//                long seconds = elapsedTimeInSeconds % 60;
//                log.info("{} Group objects fetched from Microsoft Entra ID with suffix {}", groups, configGroup.getSuffix());
//                log.info("*** <<< Done fetching all groups from Microsoft Entra ID in {} minutes and {} seconds >>> ***", minutes, seconds);
//            }).join();  // Wait for completion
//
//        } catch (ClientException e) {
//            log.error("Failed when trying to get groups. ", e);
//        }
//    }
//
//    private CompletableFuture<Integer> pageThroughGroups(CompletableFuture<GroupCollectionPage> inPageFuture) {
//        return inPageFuture.thenCompose(page -> {
//            List<CompletableFuture<Void>> futures = new ArrayList<>();
//            AtomicInteger groups = new AtomicInteger(0);
//
//            do {
//                for (Group group : page.getCurrentPage()) {
//                    if (group.displayName != null && group.displayName.endsWith(configGroup.getSuffix())) {
//                        groups.incrementAndGet();
//                        AzureGroup newGroup;
//                        try {
//                            newGroup = new AzureGroup(group, configGroup);
//                            azureGroupProducerService.publish(newGroup);
//                        } catch (NumberFormatException e) {
//                            log.warn("Problems converting resourceID to LONG! %s. Skipping creation of group", e);
//                            continue;
//                        }
//
//                        CompletableFuture<Void> memberFuture = graphService.groups(group.id).members()
//                                .buildRequest()
//                                .select("id")
//                                .getAsync()
//                                .thenCompose(memberPage -> pageThroughAzureGroupAsync(newGroup, memberPage))
//                                .exceptionally(e -> {
//                                    log.error("Error fetching page", e);
//                                    return null;
//                                });
//
//                        futures.add(memberFuture);
//                    }
//                }
//
//                CompletableFuture<GroupCollectionPage> nextPageFuture = (page.getNextPage() != null) ?
//                        page.getNextPage().buildRequest().getAsync() :
//                        CompletableFuture.completedFuture(null);
//
//                page = nextPageFuture.join();
//
//            } while (page != null);
//
//            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
//                    .thenApply(v -> groups.get());
//        });
//    }
//
//    private CompletableFuture<Void> pageThroughAzureGroupAsync(AzureGroup azureGroup, DirectoryObjectCollectionWithReferencesPage inPage) {
//        AtomicInteger members = new AtomicInteger(0); // Use AtomicInteger for thread-safe counting
//        log.debug("Fetching Azure Groups");
//
//        return CompletableFuture.supplyAsync(() -> inPage)
//                .thenCompose(page -> processPageAsync(azureGroup, page, members))
//                .thenRun(() -> log.debug("{} memberships detected in groupName {} with groupId {}",
//                        members.get(), azureGroup.getDisplayName(), azureGroup.getId()));
//    }
//
//    private CompletableFuture<Void> processPageAsync(AzureGroup azureGroup, DirectoryObjectCollectionWithReferencesPage page, AtomicInteger members) {
//        if (page == null) {
//            return CompletableFuture.completedFuture(null);
//        }
//        List<CompletableFuture<Void>> futures = page.getCurrentPage().stream()
//                .map(member -> CompletableFuture.runAsync(() -> {
//                    members.incrementAndGet();
//                    azureGroupMembershipProducerService.publishAddedMembership(new AzureGroupMembership(azureGroup.getId(), member));
//                }))
//                .toList();
//
//        CompletableFuture<DirectoryObjectCollectionWithReferencesPage> nextPageFuture = (page.getNextPage() != null) ?
//                page.getNextPage().buildRequest().getAsync() :
//                CompletableFuture.completedFuture(null);
//
//        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
//                .thenCompose(v -> nextPageFuture)
//                .thenCompose(nextPage -> processPageAsync(azureGroup, nextPage, members));
//    }


//    public void pullAllGroups() {
//        log.info("*** <<< Fetching groups from Microsoft Entra >>> ***");
//        long startTime = System.currentTimeMillis();
//
//        try {
//            CompletableFuture<GroupCollectionPage> initialPageFuture = graphService.groups()
//                    .buildRequest()
//                    .select(String.format("id,displayName,description,%s", configGroup.getFintkontrollidattribute()))
//                    .getAsync();
//
//            CompletableFuture<Integer> resultFuture = pageThroughGroups(initialPageFuture);
//            resultFuture.thenAccept(groups -> {
//                long endTime = System.currentTimeMillis();
//                long elapsedTimeInSeconds = (endTime - startTime) / 1000;
//                long minutes = elapsedTimeInSeconds / 60;
//                long seconds = elapsedTimeInSeconds % 60;
//                log.info("{} Group objects fetched from Microsoft Entra ID with suffix {}", groups, configGroup.getSuffix());
//                log.info("*** <<< Done fetching all groups from Microsoft Entra ID in {} minutes and {} seconds >>> ***", minutes, seconds);
//            }).join();  // Wait for completion
//
//        } catch (ClientException e) {
//            log.error("Failed when trying to get groups. ", e);
//        }
//    }
//
//    private CompletableFuture<Integer> pageThroughGroups(CompletableFuture<GroupCollectionPage> inPageFuture) {
//        return inPageFuture.thenCompose(page -> {
//            List<CompletableFuture<Void>> futures = new ArrayList<>();
//            int[] groups = {0};
//
//            do {
//                for (Group group : page.getCurrentPage()) {
//                    if (group.displayName != null && group.displayName.endsWith(configGroup.getSuffix())) {
//                        groups[0]++;
//                        AzureGroup newGroup;
//                        try {
//                            newGroup = new AzureGroup(group, configGroup);
//                            azureGroupProducerService.publish(newGroup);
//                        } catch (NumberFormatException e) {
//                            log.warn("Problems converting resourceID to LONG! %s. Skipping creation of group", e);
//                            continue;
//                        }
//
//                        CompletableFuture<Void> memberFuture = graphService.groups(group.id).members()
//                                .buildRequest()
//                                .select("id")
//                                .getAsync()
//                                .thenAccept(memberPage -> pageThroughAzureGroupAsync(newGroup, memberPage))
//                                //.thenRun(() -> azureGroupProducerService.publish(newGroup))
//                                .exceptionally(e -> {
//                                    log.error("Error fetching page", e);
//                                    return null;
//                                });
//
//                        futures.add(memberFuture);
//                    }
//                }
//
//                CompletableFuture<GroupCollectionPage> nextPageFuture = (page.getNextPage() != null) ?
//                        page.getNextPage().buildRequest().getAsync() :
//                        CompletableFuture.completedFuture(null);
//
//                page = nextPageFuture.join();
//
//            } while (page != null);
//
//            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
//                    .thenApply(v -> groups[0]);
//        });
//    }

//    private void pageThroughAzureGroup(AzureGroup azureGroup, DirectoryObjectCollectionWithReferencesPage inPage) {
//        int members = 0;
//        log.debug("Fetching Azure Groups");
//        DirectoryObjectCollectionWithReferencesPage page = inPage;
//        do {
//            for (DirectoryObject member : page.getCurrentPage()) {
//                members++;
//                azureGroupMembershipProducerService.publishAddedMembership(new AzureGroupMembership(azureGroup.getId(), member));
//                //azureGroup.getMembers().add(member.id);
//            }
//
//            CompletableFuture<DirectoryObjectCollectionWithReferencesPage> nextPageFuture = (page.getNextPage() != null) ?
//                    page.getNextPage().buildRequest().getAsync() :
//                    CompletableFuture.completedFuture(null);
//
//            page = nextPageFuture.join();
//
//            //page = (page.getNextPage() != null) ? page.getNextPage().buildRequest().get() : null;
//        } while (page != null);
//
//        log.debug("{} memberships detected in groupName {} with groupId {}", members, azureGroup.getDisplayName(), azureGroup.getId());
//    }

//    private CompletableFuture<Void> pageThroughAzureGroupAsync(AzureGroup azureGroup, DirectoryObjectCollectionWithReferencesPage inPage) {
//        AtomicInteger members = new AtomicInteger(0); // Use AtomicInteger for thread-safe counting
//        log.debug("Fetching Azure Groups");
//
//        return CompletableFuture.supplyAsync(() -> inPage)
//                .thenCompose(page -> processPageAsync(azureGroup, page, members))
//                .thenRun(() -> log.debug("{} memberships detected in groupName {} with groupId {}",
//                        members.get(), azureGroup.getDisplayName(), azureGroup.getId()));
//    }
//
//    private CompletableFuture<Void> processPageAsync(AzureGroup azureGroup, DirectoryObjectCollectionWithReferencesPage page, AtomicInteger members) {
//        if (page == null) {
//            return CompletableFuture.completedFuture(null);
//        }
//        List<CompletableFuture<Void>> futures = page.getCurrentPage().stream()
//                .map(member -> CompletableFuture.runAsync(() -> {
//                    members.incrementAndGet();
//                    azureGroupMembershipProducerService.publishAddedMembership(new AzureGroupMembership(azureGroup.getId(), member));
//                }))
//                .toList();
//
//        CompletableFuture<DirectoryObjectCollectionWithReferencesPage> nextPageFuture = (page.getNextPage() != null) ?
//                page.getNextPage().buildRequest().getAsync() :
//                CompletableFuture.completedFuture(null);
//
//        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
//                .thenCompose(v -> nextPageFuture)
//                .thenCompose(nextPage -> processPageAsync(azureGroup, nextPage, members));
//    }

    public boolean doesGroupExist(String resourceGroupId) {
        // TODO: Attributes should not be hard-coded [FKS-210]
        String selectionCriteria = String.format("id,displayName,description,%s", configGroup.getFintkontrollidattribute());

        GroupCollectionPage groupCollectionPage = graphService.groups()
                .buildRequest()
                .select(selectionCriteria)
                .filter(String.format(configGroup.getFintkontrollidattribute() + " eq '%s'", resourceGroupId))
                .get();

        while (groupCollectionPage != null) {
            for (Group group : groupCollectionPage.getCurrentPage()) {
                JsonElement attributeValue = group.additionalDataManager().get(configGroup.getFintkontrollidattribute());

                if (attributeValue != null && attributeValue.getAsString().equals(resourceGroupId)) {
                    return true; // Group with the specified ResourceID found
                }
            }

            // Move to the next page if available
            groupCollectionPage = groupCollectionPage.getNextPage() == null ? null :
                    groupCollectionPage.getNextPage()
                            .buildRequest()
                            .get();
        }

        return false; // Group with resourceID not found
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
                .thenAccept(createdGroup -> {
                    log.info("Added Group to Azure: {}", group.displayName);
                    azureGroupProducerService.publish(new AzureGroup(createdGroup, configGroup));
                }).exceptionally(ex -> {
                    handleGraphApiError(ex);
                    return null;
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
                            throw e; // Re-throw or handle it as needed
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

    public void updateGroup(ResourceGroup resourceGroup) {

        Group group = new MsGraphGroupMapper().toMsGraphGroup(resourceGroup, configGroup, config);
        group.owners = null;
        group.additionalDataManager().clear();

        //LinkedList<Option> requestOptions = new LinkedList<>();
        //requestOptions.add(new HeaderOption("Prefer", "create-if-missing"));

        graphService.groups(resourceGroup.getIdentityProviderGroupObjectId())
                //.buildRequest(requestOptions)
                .buildRequest()
                .patchAsync(group)
                .thenAccept(updatedGroup -> log.info("Group with GroupObjectId '{}' successfully updated", resourceGroup.getIdentityProviderGroupObjectId()))
                .exceptionally(ex -> {
                    handleGraphApiError(ex);
                    return null;
                });
    }

    public void addGroupMembership(ResourceGroupMembership resourceGroupMembership, String resourceGroupMembershipKey) {
        if(resourceGroupMembership.getAzureUserRef() != null && resourceGroupMembership.getAzureGroupRef() != null)
        {

            DirectoryObject directoryObject = new DirectoryObject();
            directoryObject.id = resourceGroupMembership.getAzureUserRef();

            try {
                graphService.groups(resourceGroupMembership.getAzureGroupRef()).members().references()
                        .buildRequest()
                        .postAsync(directoryObject)
                        .thenAccept(acceptedMember -> log.info("UserId: {} added to GroupId: {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef()));

                //log.debug("UserId {} added to GroupId {}: ", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                azureGroupMembershipProducerService.publishAddedMembership(new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject));
                log.debug("Produced message to kafka on added UserId {} to GroupId {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
            } catch (GraphServiceException e) {
                // Handle the HTTP response exception here
                if (e.getResponseCode() == 400) {
                    if(e.getError().error.message.contains("object references already exist")) {
                        azureGroupMembershipProducerService.publishAddedMembership(new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject));
                        log.debug("Republished to Kafka, UserId {} already added to GroupId {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                        return;
                    }
                    if(e.getError().error.code.contains("Request_ResourceNotFound")){
                        log.warn("AzureGroupRef is not correct on user ObjectId {} and group ObjectId {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                        return;
                    }

                    // Handle the 400 Bad Request error
                    log.warn("Bad request: ", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                    log.warn(e.getError().error.message);
                }
                if (e.getResponseCode() == 429) {
                    log.warn("Throttling limit. Error: {}", e.getError().error.message);
                }
                else {

                    // Handle other HTTP errors
                    log.error("HTTP Error while updating group {}: {} \r", resourceGroupMembership.getAzureGroupRef(), e.getError().error.message);
                }
            }
        }
    }

    public void deleteGroupMembership(String resourceGroupMembershipKey) {
        String[] splitString = resourceGroupMembershipKey.split     ("_");
        if (splitString.length != 2) {
            log.error("Key on kafka object {} not formatted correctly. NOT deleting membership from group",resourceGroupMembershipKey);
            return;
        }
        String group = splitString[0];
        String user = splitString[1];

        try {
            log.debug("trying to remove UserId: {} from GroupId: {} in Graph", user, group);

            graphService.groups(group)
                    .members(user)
                    .reference()
                    .buildRequest()
                    .deleteAsync();

            log.info("UserId: {} removed from GroupId: {}", user, group);
            azureGroupMembershipProducerService.publishDeletedMembership(resourceGroupMembershipKey);
            log.debug("Produced message to kafka on deleted UserId: {} from GroupId: {}", user, group);
        } catch (GraphServiceException e) {
            if(e.getResponseCode() == 404)
            {
                log.warn("User {} not found in group {}", user, group);
                azureGroupMembershipProducerService.publishDeletedMembership(resourceGroupMembershipKey);
                log.debug("Produced message to kafka on deleted UserId: {} from GroupId: {}", user, group);
            }
            else {
                log.error("HTTP Error while trying to remove user {} from group {}. Exception: " +
                        e.getResponseCode() + " \r" +
                        e.getError().error.message, user, group);
            }
        }
        catch (Exception e) {
            log.error("Failed to process function deleteGroupMembership, Error: ", e);
        }
    }

    private void handleGraphApiError(Throwable ex) {
        if (ex instanceof CompletionException) {
            Throwable cause = ex.getCause();
            if (cause instanceof GraphServiceException gse) {
                int statusCode = gse.getResponseCode();
                switch (statusCode) {
//                    case 204:
//                        log.info("No content response received.");
//                        break;
                    case 400:
                        log.debug("Group not created or updated. Failed with error 400");
                        break;
                    case 401:
                        log.error("Unauthorized. Check your authentication credentials");
                        break;
                    case 403:
                        log.error("Forbidden. You do not have permission to perform this action");
                        break;
                    case 404:
                        log.debug("Not found on updating group. The resource does not exist. Creating group as it is missing");
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
