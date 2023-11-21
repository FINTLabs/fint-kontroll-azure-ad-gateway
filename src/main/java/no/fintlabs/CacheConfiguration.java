package no.fintlabs;
import lombok.AllArgsConstructor;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.cache.FintCache;
import no.fintlabs.cache.FintCacheManager;
import no.fintlabs.kafka.ResourceGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
@AllArgsConstructor
@Configuration
public class CacheConfiguration {

    private final FintCacheManager fintCacheManager;

    @Bean
    FintCache<String, ResourceGroup> resourceGroupCache() {
        return createResourceCache(ResourceGroup.class);
    }

    @Bean
    FintCache<String, AzureGroup> azureGroupCache() {
        return createResourceCache(AzureGroup.class);
    }

    private <V> FintCache<String, V> createResourceCache(Class<V> resourceClass) {
        return fintCacheManager.createCache(
                resourceClass.getName().toLowerCase(Locale.ROOT),
                String.class,
                resourceClass
        );
    }
}
