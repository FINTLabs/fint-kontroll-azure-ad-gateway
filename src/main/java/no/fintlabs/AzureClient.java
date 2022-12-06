package no.fintlabs;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;

import java.util.List;

@Component
@Log4j2
@ConfigurationProperties(prefix = "azurecredentials")
public class AzureClient {
    //private final WebClient webClient;

    protected String CLIENT_ID;
    protected String CLIENT_SECRET;
    protected String TENANT_GUID;
    protected List<String> SCOPES;
    /*protected final ClientSecretCredential clientSecretCredential;*/
    /*protected final TokenCredentialAuthProvider tokenCredentialAuthProvider;*/
    protected final GraphServiceClient graphService;

    public AzureClient() {
        log.info("AzureClient initialized!");
        this.graphService = null;
    }
    //public AzureClient(WebClient webClient){
    /*public AzureClient(TokenCredentialAuthProvider tokenCredentialAuthProvider, GraphServiceClient graphService){
        this.graphService = graphService;
    //    this.webClient = webClient;
        log.info("test123");
    }*/

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

    /*@Bean
    public ClientSecretCredential clientSecretCredential() {
        return new ClientSecretCredentialBuilder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .tenantId(TENANT_GUID)
                .build();
    }*/

    /*@Bean
    public TokenCredentialAuthProvider tokenCredentialAuthProvider() {
        return new
    }*/

    @Bean
    public GraphServiceClient graphService() {
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .tenantId(TENANT_GUID)
                .build();

        TokenCredentialAuthProvider tokenCredentialAuthProvider = new TokenCredentialAuthProvider(SCOPES, clientSecretCredential);

        return GraphServiceClient
                .builder()
                .authenticationProvider(tokenCredentialAuthProvider)
                .buildClient();
    }

    @Data
    private static class LastUpdated {
        private Long lastUpdated;
    }
}
