package no.fintlabs;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.cache.FintCache;
import no.fintlabs.cache.FintCacheManager;
import no.fintlabs.kafka.ResourceGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class CacheConfiguration {


    private final FintCacheManager fintCacheManager;

    public ResourceEntityCacheConfiguration(FintCacheManager fintCacheManager) {
        this.fintCacheManager = fintCacheManager;

        @Bean
        FintCache<String, ResourceGroup> resourceGroupCache () {
            return createResourceCache(ResourceGroup.class);
        }

        @Bean
        FintCache<String, AzureGroup> azureGroupCache () {
            return createResourceCache(AzureGroup.class);
        }
    }
        private <V> FintCache<String, V> createResourceCache(Class<V> resourceClass) {
            return fintCacheManager.createCache(
                    resourceClass.getName().toLowerCase(Locale.ROOT),
                    String.class,
                    resourceClass
            );
        }
}
