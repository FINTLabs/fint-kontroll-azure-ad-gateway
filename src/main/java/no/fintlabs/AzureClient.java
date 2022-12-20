package no.fintlabs;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import lombok.Data;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import okhttp3.Request;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;

import javax.annotation.PostConstruct;
import java.util.List;


    @Component
@Log4j2
/*@Configuration
@ConfigurationProperties(prefix = "azurecredentials")*/
public class AzureClient {
    //private final WebClient webClient;

    protected String CLIENT_ID;
    protected String CLIENT_SECRET;
    protected String TENANT_GUID;
    protected List<String> SCOPES;

    protected GraphServiceClient<Request> graphService;

    public AzureClient() {
        log.info("AzureClient initialized!");
        this.CLIENT_ID = "abc";
        this.CLIENT_SECRET = "def";
        this.TENANT_GUID = "ghi";
        this.SCOPES = List<String>{};
    }
    @PostConstruct
    public void print() {
        log.info("Postconstruct log called!");
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
        if (page != null) {
            for (User user: page.getCurrentPage()) {
                log.info("User: " + user.displayName);
                log.info("Id: " + user.id);
                log.info("Mail: " + user.mail);
            }
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
    public GraphServiceClient<Request> graphService() {
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .tenantId(TENANT_GUID)
                .build();

        final TokenCredentialAuthProvider tokenCredentialAuthProvider = new TokenCredentialAuthProvider(SCOPES, clientSecretCredential);

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
