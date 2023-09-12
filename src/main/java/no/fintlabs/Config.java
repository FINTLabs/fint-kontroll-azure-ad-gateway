package no.fintlabs;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import okhttp3.Request;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//TODO: Change PostConstruct to jakarta when SB -> 3.x
//import jakarta.annotation.PostConstruct;
import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;


@Log4j2
@Setter
@Getter
@EnableAutoConfiguration
@Configuration
@ConfigurationProperties(prefix = "azurecredentials")
public class Config {
    public String clientid;
    private String clientsecret;
    private String tenantguid;
    private String tull;

    @PostConstruct
    protected void print() {
        log.info("--- test123 ---");
    }

    @Bean
    @ConfigurationProperties(prefix = "fint.flyt.azure-ad-gateway.users")
    public ConfigUser configUser() {
        return new ConfigUser();
    }


    @Bean
    @ConfigurationProperties(prefix = "azurecredentials")
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
