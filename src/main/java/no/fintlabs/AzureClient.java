package no.fintlabs;

import com.google.gson.JsonElement;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.azure.*;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.ResourceGroupMembershipConsumerService;
import okhttp3.Request;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class AzureClient {
    protected final Config config;

    protected final ConfigGroup configGroup;

    private final AzureUserProducerService azureUserProducerService;
    private final AzureUserExternalProducerService azureUserExternalProducerService;
    private final AzureGroupProducerService azureGroupProducerService;

    private final AzureGroupMembershipProducerService azureGroupMembershipProducerService;
    private final ResourceGroupMembershipConsumerService resourceGroupMembershipConsumerService;

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
                page = page.getNextPage().buildRequest().select("id").get();
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

                AzureGroup newGroup = new AzureGroup(group, config.configGroup());
                pageThrough(
                        newGroup,
                        config.graphService().groups(group.id).members()
                                .buildRequest()
                                .select("id")
                                .get()
                );
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

    private void pageThrough(UserCollectionPage inPage) {
        int users = 0;
        UserCollectionPage page = inPage;
        do {
            for (User user : page.getCurrentPage()) {
                users++;
                if (!user.additionalDataManager().isEmpty() && user.additionalDataManager().get(config.configUser().getMainorgunitidattribute()).getAsString() != null) {
                    azureUserExternalProducerService.publish(new AzureUserExternal(user, config.configUser()));
                } else {
                    azureUserProducerService.publish(new AzureUser(user, config.configUser()));
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
                config.graphService().users()
                        .buildRequest()
                        .select(String.join(",", config.configUser().AllAttributes()))
                        .filter("usertype eq 'member'")
                        .get()
        );
        log.debug("--- finished pulling resources from Azure. ---");

    }

    private void pullAllExtUsers() {
        log.debug("--- Starting to pull users with external flag from Azure --- ");
        this.pageThrough(
                config.graphService().users()
                        .buildRequest()
                        .select(String.join(",", config.configUser().AllAttributes()))
                        .filter("usertype eq 'member'")
                        //.top(10)
                        .get()
        );
        log.debug("--- finished pulling resources from Azure. ---");

    }


    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.delta-delay-ms}"
    )
    //$select=id,displayName&$expand=members($select=id,userPrincipalName,displayName)
    private void pullAllGroups() {
        log.debug("*** Fetching all groups from AD >>> ***");
        this.pageThrough(
                config.graphService().groups()
                        .buildRequest()
                        .select(String.format("id,displayName,description,members,%s", config.configGroup().getFintkontrollidattribute()))
                        .expand(String.format("members($select=%s)", String.join(",", config.configUser().AllAttributes())))
                        // TODO: Filter to only get where FintKontrollIds is set [FKS-196]
                        //.filter(String.format("%s ne null",configGroup.getFintkontrollidattribute()))
                        .get()
        );
        log.debug("*** <<< Done fetching all groups from AD ***");
    }

    public boolean doesGroupExist(String resourceGroupId) {
        // TODO: Should this be implemented as a simpler call to MS Graph? [FKS-200]
        // Form the selection criteria for the MS Graph request
        String selectionCriteria = String.format("id,displayName,description,%s", config.configGroup().getFintkontrollidattribute());

        GroupCollectionPage groupCollectionPage = config.graphService().groups()
                .buildRequest()
                .select(selectionCriteria)
                .get();

        while (groupCollectionPage != null) {
            for (Group group : groupCollectionPage.getCurrentPage()) {
                JsonElement attributeValue = group.additionalDataManager().get(config.configGroup().getFintkontrollidattribute());

                if (attributeValue != null && attributeValue.getAsString().equals(resourceGroupId)) {
                    return true; // Group with the specified ResourceID found
                }
            }

            // Move to the next page if available
            groupCollectionPage = groupCollectionPage.getNextPage() == null ? null :
                    groupCollectionPage.getNextPage()
                            .buildRequest()
                            .select(selectionCriteria)
                            .get();
        }

        return false; // Group with resourceID not found
    }

    public void addGroupToAzure(ResourceGroup resourceGroup) {
        Group group = new MsGraphGroupMapper().toMsGraphGroup(resourceGroup, config.configGroup(), config);

        log.debug("Adding Group to Azure: {}", resourceGroup.getResourceName());

        config.graphService().groups()
                .buildRequest()
                .post(group);

    }

    public void deleteGroup(String groupID) {
        config.graphService().groups(groupID)
                .buildRequest()
                .delete();
        log.debug("Group with kafkaId {} deleted ", groupID);
    }

    public void updateGroup(ResourceGroup resourceGroup) {
        // TODO: Implement actual functionality to update the group in Azure [FKS-199]
    }
}
