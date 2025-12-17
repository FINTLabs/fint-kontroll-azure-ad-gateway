package no.fintlabs.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.authentication.AzureIdentityAuthenticationProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.CoreObjectListOrchestrator;
import no.fintlabs.core.entity.CoreObject;
import no.fintlabs.core.persistence.CoreObjectListDBRepositoryImpl;
import no.fintlabs.core.persistence.CoreObjectListDbOperations;
import no.fintlabs.core.persistence.CoreObjectListPersistenceCoordinator;
import no.fintlabs.core.persistence.MSGraphPersistenceService;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@Setter
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
        private int maxpollinterval;
        private int maxretentiontime;
        //private boolean seekingOffsetResetOnAssignment;

    }

    @Bean
    CoreObjectListOrchestrator orchestrator() {
        return new CoreObjectListOrchestrator();
    }

    @Bean
    MSGraphPersistenceService msGraphPersistenceService() {
        return new MSGraphPersistenceService();
    }

    @Bean
    CoreObjectListDbOperations<UUID, CoreObject> dbRepository() {
        return new CoreObjectListDBRepositoryImpl<>();
    }

    @Bean
    CoreObjectListPersistenceCoordinator persistenceCoordinator(
            CoreObjectListDbOperations<UUID, CoreObject> dbRepository,
            MSGraphPersistenceService msGraphPersistenceService,
            CoreObjectListOrchestrator orchestrator
    ) {
        return new CoreObjectListPersistenceCoordinator(dbRepository, msGraphPersistenceService, orchestrator);
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

        okhttp3.Dispatcher dispatcher = new okhttp3.Dispatcher();
        dispatcher.setMaxRequests(128);
        dispatcher.setMaxRequestsPerHost(64);

        okhttp3.ConnectionPool pool = new okhttp3.ConnectionPool(
                100, 5, TimeUnit.MINUTES);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(pool)
                .callTimeout(timeout, TimeUnit.MINUTES)
                .connectTimeout(timeout, TimeUnit.MINUTES)
                .readTimeout(timeout, TimeUnit.MINUTES)
                .writeTimeout(timeout, TimeUnit.MINUTES)
                .retryOnConnectionFailure(true)
                .build();

        if (null == credential) {
            log.error("Unexpected error");
        }

        assert credential != null;
        return new GraphServiceClient(new AzureIdentityAuthenticationProvider(credential,new String[0], scopes), okHttpClient);
    }
}
