package no.fintlabs;

import com.azure.core.credential.TokenCredential;
import com.azure.core.util.HttpClientOptions;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import com.microsoft.kiota.authentication.AzureIdentityAuthenticationProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import com.azure.core.http.HttpClient;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(clientid)
                .tenantId(tenantguid)
                .clientSecret(clientsecret)
                .build();

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .callTimeout(3, TimeUnit.MINUTES)
                .connectTimeout(3, TimeUnit.MINUTES)
                .readTimeout(3, TimeUnit.MINUTES)
                .writeTimeout(3, TimeUnit.MINUTES)
                .build();

        if (null == scopes || null == credential) {
            log.error("Unexpected error");
        }

        assert credential != null;
        return new GraphServiceClient(new AzureIdentityAuthenticationProvider(credential,new String[0], scopes), okHttpClient);

        //return new GraphServiceClient(credential, scopes);
    }
}
