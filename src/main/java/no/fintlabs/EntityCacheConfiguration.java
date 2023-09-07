package no.fintlabs;

import no.fintlabs.cache.FintCache;
import no.fintlabs.cache.FintCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
public class EntityCacheConfiguration {

    private final FintCacheManager fintCacheManager;

    public EntityCacheConfiguration(FintCacheManager fintCacheManager) { this.fintCacheManager = fintCacheManager; }


    @Bean
    FintCache<String, AzureGroup> azureGroupResourceCache(){return createCache(AzureGroup.class);}


    @Bean
    FintCache<String, Integer> publishedGroupHashCache() {
        return createCache(Integer.class);
    }

    private <V> FintCache<String, V> createCache(Class<V> resourceClass) {
        return fintCacheManager.createCache(
                resourceClass.getName().toLowerCase(Locale.ROOT),
                String.class,
                resourceClass
        );
    }

}
