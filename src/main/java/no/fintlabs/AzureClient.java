package no.fintlabs;

import com.google.gson.JsonElement;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.*;
import jakarta.annotation.PostConstruct;
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
    private final FintCache<String, ResourceGroup> resourceGroupCache;
    private final FintCache<String, AzureGroup> azureGroupCache;

    private void pageThrough(AzureGroup azureGroup, DirectoryObjectCollectionWithReferencesPage inPage) {
        int members = 0;
        log.debug("Fetching Azure Groups");
        DirectoryObjectCollectionWithReferencesPage page = inPage;
        do {
            members++;
            for (DirectoryObject member : page.getCurrentPage()) {
                // New member detected
                azureGroupMembershipProducerService.publish(new AzureGroupMembership(azureGroup.getId(), member));
                azureGroup.getMembers().add(member.id);
            }
            if (page.getNextPage() == null) {
                break;
            } else {
                log.debug("Processing membership page");
                page = page.getNextPage().buildRequest().get();
            }
        } while (page != null);

        log.debug("{} memberships detected", members);
    }

    private void pageThrough(GroupCollectionPage inPage) {
        int groups = 0;
        GroupCollectionPage page = inPage;
        do {
            for (Group group : page.getCurrentPage()) {
                groups++;

                AzureGroup newGroup;
                try {
                    newGroup = new AzureGroup(group, configGroup);
                } catch (NumberFormatException e) {
                    log.warn("Problems converting resourceID to LONG! {}. Skipping creation of group", e);
                    continue;
                }

                // Put object into cache
                try {
                    pageThrough(
                            newGroup,
                            graphService.groups(group.id).members()
                                    .buildRequest()
                                    .select("id")
                                    .get()
                    );
                } catch (ClientException e) {
                    log.error("Error fetching page", e);
                }
                azureGroupProducerService.publish(newGroup);
            }
            if (page.getNextPage() == null) {
                break;
            } else {
                log.debug("Processing group page");
                page = page.getNextPage().buildRequest().get();
            }
        } while (page != null);
        log.debug("{} Group objects detected!", groups);
    }


    private List<AzureGroup> pageThroughGetGroups(GroupCollectionPage inPage) {
        int groups = 0;
        GroupCollectionPage page = inPage;
        List<AzureGroup> retGroupList = new ArrayList<AzureGroup>();
        do {
            for (Group group : page.getCurrentPage()) {

                AzureGroup newGroup;
                try {
                    newGroup = new AzureGroup(group, configGroup);
                } catch (NumberFormatException e) {
                    log.warn("Problems converting resourceID to LONG! {}. Skipping creation of group", e);
                    continue;
                }
                retGroupList.add(newGroup);
            }
            if (page.getNextPage() == null) {
                break;
            } else {
                log.debug("Processing group page");
                page = page.getNextPage().buildRequest().get();
            }
        } while (page != null);
        log.debug("{} Group objects detected!", groups);
        return retGroupList;
    }

    private void pageThrough(UserCollectionPage inPage) {
        int users = 0;
        UserCollectionPage page = inPage;
        do {
            for (User user : page.getCurrentPage()) {
                users++;
                if (AzureUser.getAttributeValue(user, configUser.getExternaluserattribute()) != null
                        && (AzureUser.getAttributeValue(user, configUser.getExternaluserattribute()).equalsIgnoreCase(configUser.getExternaluservalue())))
                {
                    log.debug("Adding external user to Kafka, {}",user.userPrincipalName);
                    azureUserExternalProducerService.publish(new AzureUserExternal(user, configUser));
                }
                else
                {
                    log.debug("Adding user to Kafka, {}",user.userPrincipalName);
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
        log.debug("{} User objects detected!", users);
    }

    // Fetch full user catalogue
    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.fixed-delay-ms}"
    )

    private void pullAllUsers() {
        log.debug("--- Starting to pull users from Azure --- ");
        this.pageThrough(
                graphService.users()
                        .buildRequest()
                        .select(String.join(",", configUser.AllAttributes()))
                        .filter("usertype eq 'member'")
                        .get()
        );
        log.debug("--- finished pulling resources from Azure. ---");
    }

    private void pullAllExtUsers() {
        log.debug("--- Starting to pull users with external flag from Azure --- ");
        this.pageThrough(
                graphService.users()
                        .buildRequest()
                        .select(String.join(",", configUser.AllAttributes()))
                        .filter("usertype eq 'member'")
                        //.top(10)
                        .get()
        );
        log.debug("--- finished pulling resources from Azure. ---");

    }

    public List<AzureGroup> getAllGroups() {
        return this.pageThroughGetGroups(
                graphService.groups()
                        .buildRequest()
                        .select(String.format("id,displayName,description,members,%s", configGroup.getFintkontrollidattribute()))
                        .expand(String.format("members($select=%s)", String.join(",", configUser.AllAttributes())))
                        .get()
        );
    }

    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.delta-delay-ms}"
    )
    public void pullAllGroups() {
        log.info("*** Fetching all groups from AD >>> ***");
        try {
            this.pageThrough(
                    graphService.groups()
                            .buildRequest()
                            // TODO: Attributes should not be hard-coded [FKS-210]
                            .select(String.format("id,displayName,description,members,%s", configGroup.getFintkontrollidattribute()))
                            .expand(String.format("members($select=%s)", String.join(",", configUser.AllAttributes())))
                            // TODO: Filter to only get where FintKontrollIds is set [FKS-196]
                            //.filter(String.format("%s ne null",configGroup.getFintkontrollidattribute()))
                            .get()
            );
        } catch (ClientException e) {
            log.error("Failed when trying to get groups. ", e);
        }
        log.info("*** <<< Done fetching all groups from AD ***");
    }

    public boolean doesGroupExist(String resourceGroupId) {
        // TODO: Should this be implemented as a simpler call to MS Graph? [FKS-200]
        // Form the selection criteria for the MS Graph request
        // TODO: Attributes should not be hard-coded [FKS-210]
        String selectionCriteria = String.format("id,displayName,description,%s", configGroup.getFintkontrollidattribute());

        GroupCollectionPage groupCollectionPage = graphService.groups()
                .buildRequest()
                .select(selectionCriteria)
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
                .post(group);

    }

    public void deleteGroup(String groupID) {
        graphService.groups(groupID)
                .buildRequest()
                .delete();
        log.info("Group with kafkaId {} deleted ", groupID);
    }

    public void updateGroup(ResourceGroup resourceGroup) {
        // TODO: Implement actual functionality to update the group in Azure [FKS-199]
    }

    public void addGroupMembership(ResourceGroupMembership resourceGroupMembership, String resourceGroupMembershipKey) {
        DirectoryObject directoryObject = new DirectoryObject();
        directoryObject.id = resourceGroupMembership.getAzureUserRef();

        try {
            Objects.requireNonNull(graphService.groups(resourceGroupMembership.getAzureGroupRef()).members().references())
                    .buildRequest()
                    .post(directoryObject);
            log.info("User {} added to group {}: ", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
        } catch (GraphServiceException e) {
            // Handle the HTTP response exception here
            if (e.getResponseCode() == 400) {
                // Handle the 400 Bad Request error
                log.warn("User {} already exists in group {} or azureGroupRef is not correct: ", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
            } else {
                // Handle other HTTP errors
                log.error("HTTP Error while updating group {}: " + e.getResponseCode() + " \r" + e.getResponseMessage(), resourceGroupMembership.getAzureGroupRef());
            }
        }
    }

    public void deleteGroupMembership(ResourceGroupMembership resourceGroupMembership, String resourceGroupMembershipKey) {
        String[] splitString = resourceGroupMembershipKey.split     ("_");
        if (splitString.length != 2) {
            log.error("Group index not formatted correctly. NOT deleting group");
            return;
        }
        String group = splitString[0];
        String user = splitString[1];

        try {
            Objects.requireNonNull(graphService.groups(group)
                    .members(user)
                    .reference()
                    .buildRequest()
                    .delete());
            log.warn("User: {} removed from group: {}", user, group);
        }
        catch (GraphServiceException e)
        {
            log.error("HTTP Error while removing user from group {}: " + e.getResponseCode() + " \r" + e.getResponseMessage());
        }
    }
}
