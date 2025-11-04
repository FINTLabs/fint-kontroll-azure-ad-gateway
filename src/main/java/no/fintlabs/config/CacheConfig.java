package no.fintlabs.config;

import no.fintlabs.kafka.ResourceGroupMembership;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class CacheConfig {

    @Bean
    ConcurrentHashMap<String, Optional<ResourceGroupMembership>> resourceGroupMembershipCache(){
        return new ConcurrentHashMap<>();
    }

}
