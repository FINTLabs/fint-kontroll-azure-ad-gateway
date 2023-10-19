package no.fintlabs;

import com.microsoft.graph.http.BaseCollectionPage;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.azure.*;
import no.fintlabs.kafka.ResourceGroupConsumerService;
import no.fintlabs.kafka.ResourceGroupMembershipConsumerService;
import okhttp3.Request;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class AzureClient {
    protected final GraphServiceClient<Request> graphServiceClient;
    protected final ConfigUser configUser;
    protected final ConfigGroup configGroup;
    private final AzureUserProducerService azureUserProducerService;
    private final AzureUserExternalProducerService azureUserExternalProducerService;
    private final AzureGroupProducerService azureGroupProducerService;

    private final AzureGroupMembershipProducerService azureGroupMembershipProducerService;

    private final ResourceGroupConsumerService resourceGroupConsumerService;
    private final ResourceGroupMembershipConsumerService resourceGroupMembershipConsumerService;

    private void pageThrough(AzureGroup azureGroup, DirectoryObjectCollectionWithReferencesPage inPage) {
        int members = 0;
        log.debug("Fetching Azure Groups");
        DirectoryObjectCollectionWithReferencesPage page = inPage;
        do {
            members++;
            for (DirectoryObject member: page.getCurrentPage()) {
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
            for (Group group: page.getCurrentPage()) {
                groups++;

                AzureGroup newGroup = new AzureGroup(group, configGroup);
                // TODO: Loop through all groups, and get group membership}
                pageThrough(
                        newGroup,
                        graphServiceClient.groups(group.id).members()
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
            for (User user: page.getCurrentPage()) {
                users++;
                // Todo: If external-user, call "AzureUserExternal"-class
                if (!user.additionalDataManager().isEmpty() && user.additionalDataManager().get(configUser.getMainorgunitidattribute()).getAsString() != null) {
                    azureUserExternalProducerService.publish(new AzureUserExternal(user, configUser));
                }
                else {
                    azureUserProducerService.publish(new AzureUser(user, configUser));
                }

            }
            if (page.getNextPage() == null) {
                break;
            } else {
                //log.info("Processing user page");
                page = page.getNextPage().buildRequest().get();
            }
        }while (page != null);
        log.debug("{} User objects detected!", users);

    }

    // Fetch full user catalogue
    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.fixed-delay-ms}"
    )
    private void pullAllUsers() {
        log.debug("--- Starting to pull users from Azure --- ");
        // TODO: Change to while loop (while change != null;
        // TODO: Do I need some sleep time between requests?
        this.pageThrough(
                this.graphServiceClient.users()
                        .buildRequest()
                        .select(String.join(",", configUser.AllAttributes()))
                        .filter("usertype eq 'member'")
                        //.top(10)
                        .get()
        );
        log.debug("--- finished pulling resources from Azure. ---");

    }
    private void pullAllExtUsers() {
        log.debug("--- Starting to pull users with external flag from Azure --- ");
        // TODO: Change to while loop (while change != null;
        // TODO: Do I need some sleep time between requests?
        this.pageThrough(
                this.graphServiceClient.users()
                        .buildRequest()
                        .select(String.join(",", configUser.AllAttributes()))
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
               this.graphServiceClient.groups()
                       .buildRequest()
                       .select(String.format("id,displayName,description,members,%s", configGroup.getFintkontrollidattribute()))
                       .expand(String.format("members($select=%s)",String.join(",", configUser.AllAttributes())))
                       //TODO: Filter to only get where FintKontrollIds is set
                       //.filter(String.format("%s ne null",configGroup.getFintkontrollidattribute()))
                       .get()
        );
        log.debug("*** <<< Done fetching all groups from AD ***");
    }

    private void createGroup(AzureGroup azureGroup) {
        log.debug("Azure create group: {}", azureGroup.getDisplayName());
    }

    public void run() {
        log.debug("Trigger called");
        log.debug(this.graphServiceClient.users());
    }

    private void iterPage(BaseCollectionPage collection) {
    }

}
