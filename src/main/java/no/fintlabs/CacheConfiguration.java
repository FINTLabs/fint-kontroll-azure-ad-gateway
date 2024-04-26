package no.fintlabs;
import lombok.AllArgsConstructor;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.cache.FintCache;
import no.fintlabs.cache.FintCacheManager;
import no.fintlabs.kafka.ResourceGroupMembership;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.Optional;

@AllArgsConstructor
@Configuration
public class CacheConfiguration {
/*    @Autowired
    private final AzureClient azureClient;*/
    private final FintCacheManager fintCacheManager;

    @Bean
    FintCache<String, ResourceGroup> resourceGroupCache() {
        return createResourceCache(ResourceGroup.class);
    }

    @Bean
    FintCache<String, Optional> resourceGroupMembershipCache() {
        //return createResourceCache(ClassValue<Optional<ResourceGroupMembership>>);
        return fintCacheManager.createCache(
                ResourceGroupMembership.class.getName().toLowerCase(Locale.ROOT),
                String.class,
                Optional.class
        );
    }

    @Bean
    FintCache<String, AzureGroup> azureGroupCache() {
        return createResourceCache(AzureGroup.class);
        /*List<AzureGroup> allGroups = azureClient.getAllGroups();

        FintCache<String, AzureGroup> retCache = createResourceCache(AzureGroup.class);
        for (AzureGroup group: allGroups) {
            retCache.put(group.getId(), group);
        }
        return retCache;*/
    }

    private <V> FintCache<String, V> createResourceCache(Class<V> resourceClass) {
        return fintCacheManager.createCache(
                resourceClass.getName().toLowerCase(Locale.ROOT),
                String.class,
                resourceClass
        );
    }
}
