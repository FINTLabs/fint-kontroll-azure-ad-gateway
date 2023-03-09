package no.fintlabs;

import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.*;
import lombok.Data;
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
    /*public AzureClient(GraphServiceClient<Request> graphServiceClient) {
        this.graphServiceClient = graphServiceClient;
        // TODO: Handle insufficient credentials
    }*/
    /*private final AzureUserProducer<AzureUser> azureUserProducer;*/

    @Scheduled(fixedRateString = "${fint.flyt.azure-ad-gateway.resources.refresh.interval-ms}")
    private void pulldeAllUpdatedEntities() {
    }

    private void procUserPage(UserCollectionPage page) {
        for (User user : page.getCurrentPage()) {
            log.info("***");
            log.info("  Id: " + user.id);
            log.info("  UPN: " + user.userPrincipalName);
            log.info("  Mail: " + user.mail);
            azureUserProducerService.publish(new AzureUser(user));
        }
    }

    // Fetch full user catalogue
    @Scheduled(
            initialDelayString = "${fint.flyt.azure-ad-gateway.resources.pull.initial-delay-ms}",
            fixedDelayString = "${fint.flyt.azure-ad-gateway.resources.pull.fixed-delay-ms}"
    )
    private void pullAllEntities() {
        log.info("--- Starting to pull resources from Azure --- ");
        UserCollectionPage page = this.graphServiceClient.users()
                .buildRequest()
                //.select("displayName,id,mail")
                /*.count(true)*/
                //.orderBy("displayName")
                .get();
        // TODO: Change to while loop (while change != null;
        //while (page != null) {
        // TODO: Do I need some sleep time between requests?
        do {
            this.procUserPage(page);
            UserCollectionRequestBuilder nextPage = page.getNextPage();
            if (nextPage == null) {
                break;
            } else {
                page = nextPage.buildRequest().get();
            }
        } while (page != null);
        log.info("--- finished pulling resources from Azure. ---");

        // Initialize Kafka pipelines
        //this.
    }

    public void run() {
        log.info("Trigger called");
        log.info(this.graphServiceClient.users());
    }

    @Data
    private static class LastUpdated {
        private Long lastUpdated;
    }
}
