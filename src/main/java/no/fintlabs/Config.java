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
//import jakarta.annotation.PostConstruct;
import jakarta.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;


@Log4j2
@Getter
@Setter
@EnableAutoConfiguration
@Configuration
@ConfigurationProperties(prefix = "azure.credentials")
public class Config {
    private String clientid;
    private String clientsecret;
    private String tenantguid;

    private String entobjectid;


    @PostConstruct
    protected void print() {
        log.debug("Starting PostConstruct");
    }

    @Bean
    @ConfigurationProperties(prefix = "fint.kontroll.azure-ad-gateway.users")
    public ConfigUser configUser() {
        return new ConfigUser();
    }

    @Bean
    @ConfigurationProperties(prefix = "fint.kontroll.azure-ad-gateway.group")
    public ConfigGroup configGroup() {
        return new ConfigGroup();
    }

    @Bean
    //@ConfigurationProperties(prefix = "azure.credentials")
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
