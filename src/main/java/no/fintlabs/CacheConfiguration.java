package no.fintlabs;
import lombok.AllArgsConstructor;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.azure.AzureGroupMembership;
import no.fintlabs.azure.AzureUser;
import no.fintlabs.azure.AzureUserExternal;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.cache.FintCache;
import no.fintlabs.cache.FintCacheManager;
import no.fintlabs.kafka.ResourceGroupMembership;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@AllArgsConstructor
@Configuration
public class CacheConfiguration {

    @Bean
    public ConcurrentHashMap<String, Optional<ResourceGroup>> resourceGroupCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentHashMap<String, Optional<ResourceGroupMembership>> resourceGroupMembershipCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentHashMap<String, AzureUser> entraIdUserCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentHashMap<String, AzureUserExternal> entraIdExternalUserCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentHashMap<String, AzureGroup> azureGroupCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentHashMap<String, AzureGroupMembership> azureGroupMembershipCache() {
        return new ConcurrentHashMap<>();
    }
}
