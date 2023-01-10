package no.fintlabs;

import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.Request;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Log4j2
//@ConfigurationProperties(prefix = "azurecredentials")
public class AzureClient {
    protected final GraphServiceClient<Request> graphServiceClient;
    public AzureClient(GraphServiceClient<Request> graphServiceClient) {
        this.graphServiceClient = graphServiceClient;
        // TODO: Handle insufficient credentials
    }

    @Scheduled(fixedRateString = "${fint.flyt.azure-ad-gateway.resources.refresh.interval-ms}")
    private void pulldeAllUpdatedEntities() {
    }

    private void procUserPage(UserCollectionPage page) {
        for (User user: page.getCurrentPage()) {
            log.info("***");
            log.info("  User: " + user.displayName);
            log.info("  Id: " + user.id);
            log.info("  Mail: " + user.mail);
        }
    }
        
    // Fetch full user catalogue
    @Scheduled(
            initialDelayString = "${fint.flyt.azure-ad-gateway.resources.pull.initial-delay-ms}",
            fixedDelayString = "${fint.flyt.azure-ad-gateway.resources.pull.fixed-delay-ms}"
    )
    private void pullAllUpdatedEntities() {
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
        if (page != null) {
            this.procUserPage(page);
            page = page.getNextPage()
                    .buildRequest()
                    .get();
        }
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
