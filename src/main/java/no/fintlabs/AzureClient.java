package no.fintlabs;

import com.google.gson.JsonElement;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import com.microsoft.graph.options.HeaderOption;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.requests.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.azure.*;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.ResourceGroupMembership;
import okhttp3.Request;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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
    private long startTime;

    private final AzureGroupMembershipProducerService azureGroupMembershipProducerService;
    private final FintCache<String, ResourceGroup> resourceGroupCache;
    private final FintCache<String, AzureGroup> azureGroupCache;
/*
//    private void pageThroughAzureGroup(AzureGroup azureGroup, CompletableFuture<DirectoryObjectCollectionWithReferencesPage> inPageFuture) {
//        inPageFuture.thenAccept(inPage -> {
//            int members = 0;
//            log.debug("Fetching Azure Groups");
//            DirectoryObjectCollectionWithReferencesPage page = inPage;
//            do {
//                for (DirectoryObject member : page.getCurrentPage()) {
//                    // New member detected
//                    members++;
//                    azureGroupMembershipProducerService.publishAddedMembership(new AzureGroupMembership(azureGroup.getId(), member));
//                    azureGroup.getMembers().add(member.id);
//                }
//                if (page.getNextPage() == null) {
//                    break;
//                } else {
//                    log.debug("Processing membership page");
//                    page = page.getNextPage().buildRequest().get();
//                }
//            } while (page != null);
//
//            log.debug("{} memberships detected in groupName {} with groupId {}", members, azureGroup.getDisplayName(), azureGroup.getId());
//        } );
//    }
*/
/*
//    private void pageThroughGroups(CompletableFuture<GroupCollectionPage> inPageFuture) {
//        inPageFuture.thenAccept(inPage -> {
//            int groups = 0;
//            GroupCollectionPage page = inPage;
//            do {
//                for (Group group : page.getCurrentPage()) {
//
//                    if (group.displayName != null && group.displayName.endsWith(configGroup.getSuffix())) {
//                        groups++;
//                        AzureGroup newGroup;
//                        try {
//                            newGroup = new AzureGroup(group, configGroup);
//                        } catch (NumberFormatException e) {
//                            log.warn("Problems converting resourceID to LONG! {}. Skipping creation of group", e);
//                            continue;
//                        }
//
//                        // Put object into cache
//                        try {
//                            pageThroughAzureGroup(
//                                    newGroup,
//                                    graphService.groups(group.id).members()
//                                            .buildRequest()
//                                            .select("id")
//                                            .getAsync()
//                            );
//                        } catch (ClientException e) {
//                            log.error("Error fetching page", e);
//                        }
//                        azureGroupProducerService.publish(newGroup);
//                    }
//                }
//                if (page.getNextPage() == null) {
//                    break;
//                } else {
//                    log.debug("Processing group page");
//                    page = page.getNextPage().buildRequest().get();
//                }
//            } while (page != null);
//
//            log.info("{} Group objects detected in Microsoft Entra", groups);
//
//        } );
//
//    }
/*
/*
//    private List<AzureGroup> pageThroughGetGroups(GroupCollectionPage inPage) {
//        int groups = 0;
//        GroupCollectionPage page = inPage;
//        List<AzureGroup> retGroupList = new ArrayList<AzureGroup>();
//        do {
//            for (Group group : page.getCurrentPage()) {
//
//                AzureGroup newGroup;
//                try {
//                    newGroup = new AzureGroup(group, configGroup);
//                } catch (NumberFormatException e) {
//                    log.warn("Problems converting resourceID to LONG! {}. Skipping creation of group", e);
//                    continue;
//                }
//                retGroupList.add(newGroup);
//            }
//            if (page.getNextPage() == null) {
//                break;
//            } else {
//                log.debug("Processing group page");
//                page = page.getNextPage().buildRequest().get();
//            }
//        } while (page != null);
//        log.debug("{} Group objects detected in Microsoft Entra", groups);
//        return retGroupList;
//    }
*/
    private void pageThroughUsers(UserCollectionPage inPage) {
        //inPageFuture.thenAccept(inPage -> {
            int users = 0;

            UserCollectionPage page = inPage;
            do {
                for (User user : page.getCurrentPage()) {
                    users++;
                    if (AzureUser.getAttributeValue(user, configUser.getExternaluserattribute()) != null
                            && (AzureUser.getAttributeValue(user, configUser.getExternaluserattribute()).equalsIgnoreCase(configUser.getExternaluservalue()))) {
                        log.debug("Adding external user to Kafka, {}", user.userPrincipalName);
                        azureUserExternalProducerService.publish(new AzureUserExternal(user, configUser));
                    } else {
                        log.debug("Adding user to Kafka, {}", user.userPrincipalName);
                        azureUserProducerService.publish(new AzureUser(user, configUser));
                    }
                }
                if (page.getNextPage() == null) {
                    break;
                } else {
                    //log.info("Processing user page");
                    page = page.getNextPage().buildRequest().get();
                }
            } while (page != null);
            log.info("{} User objects detected in Microsoft Entra", users);
        //});
    }

    // Fetch full user catalogue
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
        }
        catch (ClientException ex) {}
    }

/*
//    private void pullAllExtUsers() {
//        log.debug("--- Starting to pull users with external flag from Azure --- ");
//        this.pageThrough(
//                graphService.users()
//                        .buildRequest()
//                        .select(String.join(",", configUser.AllAttributes()))
//                        .filter("usertype eq 'member'")
//                        //.top(10)
//                        .get()
//        );
//        log.debug("--- finished pulling resources from Azure. ---");
//
//    }
//
//    public List<AzureGroup> getAllGroups() {
//        return this.pageThroughGetGroups(
//                graphService.groups()
//                        .buildRequest()
//                        .select(String.format("id,displayName,description,members,%s", configGroup.getFintkontrollidattribute()))
//                        //.filter(String.format("startsWith(displayName,'%s')",configGroup.getPrefix()))
//                        .expand(String.format("members($select=%s)", String.join(",", configUser.AllAttributes())))
//                        .get()
//        );
//    }
*/
    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.delta-delay-ms}"
    )
    public void pullAllGroups() {
        log.info("*** <<< Fetching groups from Microsoft Entra >>> ***");
        long startTime = System.currentTimeMillis();

        try {
            CompletableFuture<GroupCollectionPage> initialPageFuture = graphService.groups()
                    .buildRequest()
                    .select(String.format("id,displayName,description,members,%s", configGroup.getFintkontrollidattribute()))
                    .getAsync();

            CompletableFuture<Integer> resultFuture = pageThroughGroups(initialPageFuture);
            resultFuture.thenAccept(groups -> {
                long endTime = System.currentTimeMillis();
                long elapsedTimeInSeconds = (endTime - startTime) / 1000;
                long minutes = elapsedTimeInSeconds / 60;
                long seconds = elapsedTimeInSeconds % 60;
                log.info("{} Group objects fetched from Microsoft Entra ID with suffix {}", groups, configGroup.getSuffix());
                log.info("*** <<< Done fetching all groups from Microsoft Entra ID in {} minutes and {} seconds >>> ***", minutes, seconds);
            }).join();  // Wait for completion

        } catch (ClientException e) {
            log.error("Failed when trying to get groups. ", e);
        }
    }

    private CompletableFuture<Integer> pageThroughGroups(CompletableFuture<GroupCollectionPage> inPageFuture) {
        return inPageFuture.thenCompose(page -> {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            int[] groups = {0};

            do {
                for (Group group : page.getCurrentPage()) {
                    if (group.displayName != null && group.displayName.endsWith(configGroup.getSuffix())) {
                        groups[0]++;
                        AzureGroup newGroup;
                        try {
                            newGroup = new AzureGroup(group, configGroup);
                        } catch (NumberFormatException e) {
                            log.warn("Problems converting resourceID to LONG! {}. Skipping creation of group", e);
                            continue;
                        }

                        CompletableFuture<Void> memberFuture = graphService.groups(group.id).members()
                                .buildRequest()
                                .select("id")
                                .getAsync()
                                .thenAccept(memberPage -> pageThroughAzureGroup(newGroup, memberPage))
                                .thenRun(() -> azureGroupProducerService.publish(newGroup))
                                .exceptionally(e -> {
                                    log.error("Error fetching page", e);
                                    return null;
                                });

                        futures.add(memberFuture);
                    }
                }

                CompletableFuture<GroupCollectionPage> nextPageFuture = (page.getNextPage() != null) ?
                        page.getNextPage().buildRequest().getAsync() :
                        CompletableFuture.completedFuture(null);

                page = nextPageFuture.join();

            } while (page != null);

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> groups[0]);
        });
    }

    private void pageThroughAzureGroup(AzureGroup azureGroup, DirectoryObjectCollectionWithReferencesPage inPage) {
        int members = 0;
        log.debug("Fetching Azure Groups");
        DirectoryObjectCollectionWithReferencesPage page = inPage;
        do {
            for (DirectoryObject member : page.getCurrentPage()) {
                members++;
                azureGroupMembershipProducerService.publishAddedMembership(new AzureGroupMembership(azureGroup.getId(), member));
                azureGroup.getMembers().add(member.id);
            }
            page = (page.getNextPage() != null) ? page.getNextPage().buildRequest().get() : null;
        } while (page != null);

        log.debug("{} memberships detected in groupName {} with groupId {}", members, azureGroup.getDisplayName(), azureGroup.getId());
    }
/*
//    public void pullAllGroups() {
//        log.info("*** <<< Fetching groups from Microsoft Entra >>> ***");
//        long startTime = System.currentTimeMillis();
//        try {
//            this.pageThroughGroups(
//                    graphService.groups()
//                            .buildRequest()
//                            // TODO: Attributes should not be hard-coded [FKS-210]
//                            .select(String.format("id,displayName,description,members,%s", configGroup.getFintkontrollidattribute()))
//                            // TODO: Improve MS Graph filter ? [FKS-687]
//                            //.filter(String.format("displayName ne null",configGroup.getResourceGroupIDattribute()))
//                            //.filter(String.format("startsWith(displayName,'%s')",configGroup.getPrefix()))
//                            // No need for expand as members are handled by pageThroughAzureGroup
//                            //.expand(String.format("members($select=%s)", String.join(",", configUser.AllAttributes())))
//                            .getAsync()
//            );
//        } catch (ClientException e) {
//            log.error("Failed when trying to get groups. ", e);
//        }
//        long endTime = System.currentTimeMillis();
//        long elapsedTimeInSeconds = (endTime - startTime) / 1000;
//        long minutes = elapsedTimeInSeconds / 60;
//        long seconds = elapsedTimeInSeconds % 60;
//
//        log.info("*** <<< Done fetching all groups from Microsoft Entra in {} minutes and {} seconds >>> ***", minutes, seconds);
//    }
*/

    public boolean doesGroupExist(String resourceGroupId) {
        // TODO: Should this be implemented as a simpler call to MS Graph? [FKS-200]
        // Form the selection criteria for the MS Graph request
        // TODO: Attributes should not be hard-coded [FKS-210]
        String selectionCriteria = String.format("id,displayName,description,%s", configGroup.getFintkontrollidattribute());

        GroupCollectionPage groupCollectionPage = graphService.groups()
                .buildRequest()
                .select(selectionCriteria)
                //.filter(String.format("startsWith(displayName,'%s')",configGroup.getPrefix()))
                //.filter(String.format("endsWith(displayName,'%s')",configGroup.getSuffix()))
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

        log.info("Adding Group to Azure: {}", resourceGroup.getResourceName());

        graphService.groups()
                .buildRequest()
                .postAsync(group);

    }

    public void deleteGroup(String groupID) {
        graphService.groups(groupID)
                .buildRequest()
                .delete();
        log.info("Group with kafkaId {} deleted ", groupID);
    }

    public void updateGroup(ResourceGroup resourceGroup) {

        Group group = new MsGraphGroupMapper().toMsGraphGroup(resourceGroup, configGroup, config);
        group.owners = null;
        group.additionalDataManager().clear();
        graphService.groups(resourceGroup.getIdentityProviderGroupObjectId())
                .buildRequest()
                .patchAsync(group);
    }

    public void addGroupMembership(ResourceGroupMembership resourceGroupMembership, String resourceGroupMembershipKey) {
        if(resourceGroupMembership.getAzureUserRef() != null && resourceGroupMembership.getAzureGroupRef() != null)
        {

            DirectoryObject directoryObject = new DirectoryObject();
            directoryObject.id = resourceGroupMembership.getAzureUserRef();

            try {
                Objects.requireNonNull(graphService.groups(resourceGroupMembership.getAzureGroupRef()).members().references())
                        .buildRequest()
                        .postAsync(directoryObject);
                log.info("UserId {} added to GroupId {}: ", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                azureGroupMembershipProducerService.publishAddedMembership(new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject));
                log.debug("Produced message to kafka on added UserId {} to GroupId {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
            } catch (GraphServiceException e) {
                // Handle the HTTP response exception here
                if (e.getResponseCode() == 400) {
                    if(e.getError().error.message.contains("object references already exist")) {
                        azureGroupMembershipProducerService.publishAddedMembership(new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject));
                        log.info("Republished to Kafka, UserId {} already added to GroupId {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                        return;
                    }
                    if(e.getError().error.code.contains("Request_ResourceNotFound")){
                        log.warn("AzureGroupRef is not correct: ", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                        return;
                    }

                    // Handle the 400 Bad Request error
                    log.warn("Bad request: ", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                    log.info(e.getError().error.message);
                }
                if (e.getResponseCode() == 429) {
                    log.warn("Throttling limit. Error: {}", e.getError().error.message);
                }
                else {

                    // Handle other HTTP errors
                    log.error("HTTP Error while updating group {}: " + e.getError().error.message + " \r", resourceGroupMembership.getAzureGroupRef());
                }
            }
        }
    }

    public void deleteGroupMembership(ResourceGroupMembership resourceGroupMembership, String resourceGroupMembershipKey) {
        String[] splitString = resourceGroupMembershipKey.split     ("_");
        if (splitString.length != 2) {
            log.error("Key on kafka object {} not formatted correctly. NOT deleting membership from group",resourceGroupMembershipKey);
            return;
        }
        String group = splitString[0];
        String user = splitString[1];

        try {
            log.info("Removing UserId: {} from GroupId: {} in Graph", user, group);

            Objects.requireNonNull(graphService.groups(group)
                                           .members(user)
                                           .reference()
                                           .buildRequest()
                                           .deleteAsync());

            log.warn("UserId: {} removed from GroupId: {} in Graph", user, group);
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
}
