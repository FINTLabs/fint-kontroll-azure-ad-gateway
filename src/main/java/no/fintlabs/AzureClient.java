package no.fintlabs;

import com.microsoft.graph.http.BaseCollectionPage;
import com.microsoft.graph.http.BaseRequestBuilder;
import com.microsoft.graph.http.DeltaCollectionPage;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
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
    private final AzureUserProducerService azureUserProducerService;
    private final AzureGroupProducerService azureGroupProducerService;

    private <T> void procObject(T object) {
        if (object instanceof User) {
            log.info("USER object detected!");
            azureUserProducerService.publish(new AzureUser((User)object));
        } else if (object instanceof Group) {
            log.info("USER object detected!");
            azureGroupProducerService.publish(new AzureGroup((Group)object));
        } else {
            log.info("ERROR unknown object detected!");
        }
    }

    private <T1, T2 extends BaseRequestBuilder<T1>> void procPage(DeltaCollectionPage<T1, T2> page) {
        for (T1 object : page.getCurrentPage()) {
            this.procObject(object);
        }
    }

    private <T1, T2 extends BaseRequestBuilder<T1>> void pageThrough(DeltaCollectionPage<T1, T2> inPage) {
        DeltaCollectionPage<T1, T2> page = inPage;
        do {
            this.procPage(page);
            T2 nextPage = page.getNextPage();
            if (nextPage == null) {
                break;
            } else {
                log.info("test123");
                page = nextPage;
                //page = nextPage.buildRequest().get();
            }
        } while (page != null);
    }

    // Fetch full user catalogue
    @Scheduled(
            initialDelayString = "${fint.flyt.azure-ad-gateway.users.pull.initial-delay-ms}",
            fixedDelayString = "${fint.flyt.azure-ad-gateway.users.pull.fixed-delay-ms}"
    )
    private void pullAllEntities() {
        log.info("--- Starting to pull resources from Azure --- ");
        // TODO: Change to while loop (while change != null;
        // TODO: Do I need some sleep time between requests?
        this.pageThrough(
                this.graphServiceClient.users()
                        .delta()
                        .buildRequest()
                        .get()
        );
        log.info("--- finished pulling resources from Azure. ---");

    }

    @Scheduled(
        initialDelayString = "${fint.flyt.azure-ad-gateway.groups.pull.initial-delay-ms}",
        fixedDelayString = "${fint.flyt.azure-ad-gateway.groups.pull.delta-delay-ms}"
    )
    private void pullAllGroups() {
        log.info("*** Fetching all groups from AD >>> ***");
        this.pageThrough(
               this.graphServiceClient.groups()
                       .delta()
                       .buildRequest()
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
