package no.fintlabs;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.authentication.AzureIdentityAuthenticationProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Log4j2
@Getter
@Setter
@EnableAutoConfiguration
@Configuration
@ConfigurationProperties(prefix = "azure")
public class Config {

    private int timeout;

    private Credentials credentials = new Credentials();

    @Getter
    @Setter
    public static class Credentials {
        private String clientid;
        private String clientsecret;
        private String tenantguid;
        private String entobjectid;
    }

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

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "fint.kafka")
    public static class KafkaConfig {
        private int maxpollrecords;
        private boolean seekingOffsetResetOnAssignment;

    }

    @Bean
    public GraphServiceClient graphServiceClient(){
        log.debug("Starting PostConstruct of GraphServiceClient");
        String[] scopes = new String[] {"https://graph.microsoft.com/.default"};


        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(credentials.getClientid())
                .tenantId(credentials.getTenantguid())
                .clientSecret(credentials.getClientsecret())
                .build();

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .callTimeout(timeout, TimeUnit.MINUTES)
                .connectTimeout(timeout, TimeUnit.MINUTES)
                .readTimeout(timeout, TimeUnit.MINUTES)
                .writeTimeout(timeout, TimeUnit.MINUTES)
                .retryOnConnectionFailure(true)
                .build();

        if (null == scopes || null == credential) {
            log.error("Unexpected error");
        }

        assert credential != null;
        return new GraphServiceClient(new AzureIdentityAuthenticationProvider(credential,new String[0], scopes), okHttpClient);
    }
}
