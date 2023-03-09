package no.fintlabs;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import okhttp3.Request;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;


@Log4j2
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "azurecredentials")
public class Config {
    private String clientid;
    private String clientsecret;
    private String tenantguid;
    private String tull;

    @PostConstruct
    protected void print() {
        log.info("--- test123 ---");
    }

    @Bean
    public GraphServiceClient<Request> graphService() {
        List<String> scopes = Arrays.asList("https://graph.microsoft.com/.default");
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(clientid)
                .clientSecret(clientsecret)
                .tenantId(tenantguid)
                .build();

        final TokenCredentialAuthProvider tokenCredentialAuthProvider = new TokenCredentialAuthProvider(scopes, clientSecretCredential);

        return GraphServiceClient
                .builder()
                .authenticationProvider(tokenCredentialAuthProvider)
                .buildClient();
    }
}
