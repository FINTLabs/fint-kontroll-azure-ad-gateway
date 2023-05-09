package no.fintlabs;

import com.microsoft.graph.http.BaseCollectionPage;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.UserCollectionPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import okhttp3.Request;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Log4j2
//@ConfigurationProperties(prefix = "azurecredentials")
@RequiredArgsConstructor
public class AzureClient {
    protected final GraphServiceClient<Request> graphServiceClient;
    protected final ConfigUser configUser;
    private final AzureUserProducerService azureUserProducerService;
    private final AzureGroupProducerService azureGroupProducerService;

    private final AzureGroupMembershipProducerService azureGroupMembershipProducerService;

    /*private final ProducerService<AzureUser> azureUserProducerService;
    private final ProducerService<AzureGroup> azureGroupProducerService;*/

    /*private <T> void procObject(T object) {
        if (object instanceof User) {
            log.info("USER object detected!");
            azureUserProducerService.publish(new AzureUser((User)object));
        } else if (object instanceof Group) {
            log.info("GROUP object detected!");
            azureGroupProducerService.publish(new AzureGroup((Group)object));
        } else if (object instanceof GroupMembers) {
            log.info("GROUP object detected!");
            azureGroupMembershipProducerService(new AzureGroupMembership(GroupMembers)object))
        } else {
            log.info("ERROR unknown object detected!");
        }
    }*/
/*
     private <T> void procObject(T object) {
        if (object instanceof User) {
            log.info("USER object detected!");
            azureUserProducerService.publish(new AzureUser((User)object));
        } else {
            log.info("ERROR unknown object detected!");
        }
    }*/

    /*private <T1, T2 extends BaseRequestBuilder<T1>> void procPage(BaseCollectionPage<T1, T2> page) {
        for (T1 object : page.getCurrentPage()) {
            this.procObject(object);
        }
    }*/

    private void pageThrough(GroupCollectionPage inPage) {
        GroupCollectionPage page = inPage;
        do {
            for (Group group: page.getCurrentPage()) {
                log.info("GROUP object detected!");
                azureGroupProducerService.publish(new AzureGroup(group));
                // TODO: Loop through all groups, and get group membership
                /*pageThrough(
                        graphServiceClient.groups(group.id).memberOf()
                        .buildRequest()
                        .get()
                        );*/
            }
            if (page.getNextPage() == null) {
                break;
            } else {
                log.info("Processing group page");
                page = page.getNextPage().buildRequest().get();
            }
        } while (page != null);
    }

    /*private void pageThrough( inPage) {
        BaseCollectionPage<T1, T2> page = inPage;
        do {
            this.procPage(page);
            if (page.getNextPage() == null) {
                break;
            } else {
                page = page.getNextPage();
                //log.info("test123");
                //page = nextPage
                //page.buildRequest().get()
            }
        } while (page != null);
    }*/

    private void pageThrough(UserCollectionPage inPage) {
        UserCollectionPage page = inPage;
        do {
            for (User user: page.getCurrentPage()) {
                log.info("USER object detected!");
                azureUserProducerService.publish(new AzureUser(user, configUser));
            }
            if (page.getNextPage() == null) {
                break;
            } else {
                log.info("Processing user page");
                page = page.getNextPage().buildRequest().get();
            }
        } while (page != null);
    }

    /*private void pageThrough(GroupCollectionPage inPage) {
        GroupCollectionPage page = inPage;
        do {
            this.procPage(page);
            if (page.getNextPage() == null) {
                break;
            } else {
                log.info("Processing user page");
                page = page.getNextPage().buildRequest().get();
            }
        } while (page != null);
    }*/

    /*private <T1, T2 extends BaseRequestBuilder<T1>> void pageThrough(BaseCollectionPage<T1, T2> inPage) {
        BaseCollectionPage<T1, T2> page = inPage;
        do {
            this.procPage(page);
            if (page.getNextPage() == null) {
                break;
            } else {
                page = page.getNextPage()
                //log.info("test123");
                //page = nextPage
                //page.buildRequest().get()
            }
        } while (page != null);
    }*/

    // Fetch full user catalogue
    @Scheduled(
            initialDelayString = "${fint.flyt.azure-ad-gateway.user-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.flyt.azure-ad-gateway.user-scheduler.pull.fixed-delay-ms}"
    )
    private void pullAllUsers() {
        log.info("--- Starting to pull users from Azure --- ");
        // TODO: Change to while loop (while change != null;
        // TODO: Do I need some sleep time between requests?
        this.pageThrough(
                this.graphServiceClient.users()
                        .buildRequest()
                        .select(String.join(",", configUser.AllAttributes()))
                        .get()
        );
        log.info("--- finished pulling resources from Azure. ---");

    }


/*    @Scheduled(
            initialDelayString = "${fint.flyt.azure-ad-gateway.users.pull.initial-delay-ms}",
            fixedDelayString = "${fint.flyt.azure-ad-gateway.users.pull.fixed-delay-ms}"
    )
    private void pullAllUsers() {
        log.info("--- Starting to pull group members from Azure --- ");
        // TODO: Change to while loop (while change != null;
        // TODO: Do I need some sleep time between requests?
        this.pageThrough(
                this.graphServiceClient.builder().a
                        .buildRequest()
                        .get()
        );
        log.info("--- finished pulling resources from Azure. ---");

    }*/

    @Scheduled(
        initialDelayString = "${fint.flyt.azure-ad-gateway.group-scheduler.pull.initial-delay-ms}",
        fixedDelayString = "${fint.flyt.azure-ad-gateway.group-scheduler.pull.delta-delay-ms}"
    )

    //$select=id,displayName&$expand=members($select=id,userPrincipalName,displayName)
    private void pullAllGroups() {
        log.info("*** Fetching all groups from AD >>> ***");
        this.pageThrough(
               this.graphServiceClient.groups()
                       .buildRequest()
                       .select("id,displayName,assignedLabels")
                       .expand(String.format("members($select=%s)",String.join(",", configUser.AllAttributes())))
                       .get()
        );
        log.info("*** <<< Done fetching all groups from AD ***");
    }


    public void run() {
        log.info("Trigger called");
        log.info(this.graphServiceClient.users());
    }

    private void iterPage(BaseCollectionPage collection) {
    }

}
