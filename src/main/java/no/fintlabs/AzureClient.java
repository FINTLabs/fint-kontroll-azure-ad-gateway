package no.fintlabs;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;

import java.util.Optional;

@Component
@Log4j2
public class AzureClient {
    //private final WebClient webClient;


    protected final ClientSecretCredential clientSecretCredential;
    protected final TokenCredentialAuthProvider tokenCredentialAuthProvider;
    protected final GraphServiceClient graphService;

    //public AzureClient(WebClient webClient){
    public AzureClient(TokenCredentialAuthProvider tokenCredentialAuthProvider, GraphServiceClient graphService){
        this.graphService = graphService;
    //    this.webClient = webClient;
        log.info("test123");
    }

    // Fetch full user catalogue
    @Scheduled(fixedRate=5000)
    public void run() {
        log.info("Trigger called");
        log.info(this.graphService.users());
        // Populate graph with data

        // https://learn.microsoft.com/en-us/graph/tutorials/java?tabs=aad&tutorial-step=9
        // Fetch incremental user catalogue
        UserCollectionPage page = this.graphService.users()
                .buildRequest()
                .select("displayName,id,mail")
                .top(25)
                .orderBy("displayName")
                .get();

        for (User user: page.getCurrentPage()) {
            log.info("User: " + user.displayName);
            log.info("Id: " + user.id);
            log.info("Mail: ", user.mail);
        }
    }

    @Bean
    public ClientSecretCredential clientSecretCredential() {
        return new ClientSecretCredentialBuilder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .tenantId(TENANT_GUID)
                .build();
    }

    @Bean
    public TokenCredentialAuthProvider tokenCredentialAuthProvider() {
        return new TokenCredentialAuthProvider(SCOPES, this.clientSecretCredential);
    }

    @Bean
    //public GraphServiceClient graphService(WebClient.Builder builder, Optional<OAuth2AuthorizedClientManager> authorizedClientManager, ClientHttpConnector clientHttpConnector) {
    public GraphServiceClient graphService() {

        return GraphServiceClient
                .builder()
                .buildClient();
    }

    @Data
    private static class LastUpdated {
        private Long lastUpdated;
    }
}
