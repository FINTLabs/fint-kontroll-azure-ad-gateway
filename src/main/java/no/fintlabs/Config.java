package no.fintlabs;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.http.CoreHttpProvider;
import com.microsoft.graph.http.IHttpProvider;
import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.logger.ILogger;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.serializer.DefaultSerializer;
import com.microsoft.graph.serializer.ISerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import jakarta.annotation.PostConstruct;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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

    private IHttpProvider<Request> httpProvider;

    @PostConstruct
    protected void print() {
        log.debug("Starting PostConstruct");
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


        // Create a custom HTTP provider with adjusted timeout
        /*okhttp3.OkHttpClient okHttpClient = new okhttp3.OkHttpClient.Builder()
                .readTimeout(100, TimeUnit.MILLISECONDS)  // Set read timeout to 30 seconds
                .authenticator(new Authenticator() {
                    @Nullable
                    @Override
                    public Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {
                        return null;
                    }
                })
                .build();*/

        /*ILogger httpLogger = new DefaultLogger();
        ISerializer httpSerializer = new DefaultSerializer(httpLogger);

        this.httpProvider =  new CoreHttpProvider(httpSerializer, httpLogger, okHttpClient);*/

        GraphServiceClient graphServiceClient = GraphServiceClient
                .builder()
                .authenticationProvider(tokenCredentialAuthProvider)
                //.httpProvider(this.httpProvider)
                .buildClient();

        //graphServiceClient.getHttpProvider().

        return graphServiceClient;

    }
}
