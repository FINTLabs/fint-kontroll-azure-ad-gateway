package no.fintlabs;

import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.util.HttpClientOptions;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.core.requests.GraphClientFactory;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import com.azure.core.http.HttpClient;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

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

    @Bean
    @ConfigurationProperties(prefix = "fint.kontroll.azure-ad-gateway.user")
    public ConfigUser configUser() {
        return new ConfigUser();
    }

    @Bean
    @ConfigurationProperties(prefix = "fint.kontroll.azure-ad-gateway.group")
    public ConfigGroup configGroup() {
        return new ConfigGroup();
    }

    @Bean
    public GraphServiceClient graphServiceClient(){
        log.debug("Starting PostConstruct of GraphServiceClient");
        String[] scopes = new String[] {"https://graph.microsoft.com/.default"};

        HttpClientOptions options = new HttpClientOptions();
        options.setConnectTimeout(Duration.ofMinutes(5));
        options.setReadTimeout(Duration.ofMinutes(5));
        options.setWriteTimeout(Duration.ofMinutes(5));

        HttpClient httpClient = HttpClient.createDefault(options);

        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .httpClient(httpClient)
                .clientId(clientid)
                .tenantId(tenantguid)
                .clientSecret(clientsecret)
                .build();

        if (null == scopes || null == credential) {
            log.error("Unexpected error");
        }

        return new GraphServiceClient(credential, scopes);
    }
}
