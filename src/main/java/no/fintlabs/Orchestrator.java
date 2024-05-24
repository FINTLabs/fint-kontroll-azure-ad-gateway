package no.fintlabs;

import com.sun.jna.platform.win32.Guid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azure.EntraGroupMembership;
import no.fintlabs.azure.EntraGroup;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.ResourceGroupMembership;
import no.fintlabs.kafka.ResourceGroupMembershipDownloader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.http.*;

import java.util.*;


@Component
@AllArgsConstructor
@Slf4j

public class Orchestrator {
    @Autowired
    private final EntraClient entraClient;
    @Autowired
    private final FintCache azureGroupCache;
    @Autowired
    private final EntraGroupMembership entraGroupMembership;
    @Autowired
    private final ResourceGroupMembershipDownloader resourceGroupMembershipDownloader;
    @Autowired
    private final ResourceGroupMembershipDownloader resourceGroupMembershipFullSink;


    private List<EntraGroup> entraGroupList;

    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "15000"
    )
    private void pullAllMemberships()
    {
        if (resourceGroupMembershipDownloader.callApi().equals(HttpStatusCode.valueOf(200)))
        {
            resourceGroupMembershipDownloader.GetFullMembershipFromKafka();
            resourceGroupMembershipDownloader.resourceGroupMembershipsFullCache.getAll();
            entraGroupList = entraClient.getAllGroups();
            for (EntraGroup entraGroup : entraGroupList) {
                // Maybe change to a members cache and not a for each member of group
                for (String member : entraGroup.getMembers())
                {
                    if (resourceGroupMembershipDownloader.resourceGroupMembershipsFullCache.containsKey(member))
                    {
                        //TODO: Compare Cache from Kafka with Entra Cache.
                         //resourceGroupMembershipDownloader.resourceGroupMembershipsFullCache.get(entraGroupMembership.getId()).
                    }

//                keyAndResourceGroupMembership -> updateAzureWithMembership(keyAndResourceGroupMembership.getT1(), keyAndResourceGroupMembership.getT2())
                }
            }
        }
    }
    public List<EntraGroupMembership> findUnmatchedUsers(List<EntraGroupMembership> entraGroupMemberships, FintCache<String, Optional> resourceGroupMemberships) {
        List<EntraGroupMembership> unmatchedUsers = new ArrayList<>();

        for (EntraGroupMembership entraGroupMembership : entraGroupMemberships) {
            String userId = entraGroupMembership.getUser_id();
            boolean userFound = false;

            // Iterate over resourceGroupMemberships to check if userId exists in azureUserRef
            for (Optional<ResourceGroupMembership> optionalMembership : resourceGroupMemberships.getAll()) {
                if (optionalMembership.isPresent()) {
                    ResourceGroupMembership membership = optionalMembership.get();
                    if (membership.getAzureUserRef().equals(userId)) {
                        userFound = true;
                        break;
                    }
                }
            }

            // If user is not found, add the AzureGroupMembership to unmatchedUsers
            if (!userFound) {
                unmatchedUsers.add(entraGroupMembership);
            }
        }

        return unmatchedUsers;
    }

//    Sink.resourceGroupMembershipFullSink.asFlux().subscribe(
//            keyAndResourceGroupMembershipFull -> updateAzureWithMembershipAsync(keyAndResourceGroupMembershipFull.getT1(), keyAndResourceGroupMembershipFull.getT2())
//            );


    private void updateAzureWithMembershipAsync(String kafkakKey, Optional<ResourceGroupMembership> resourceGroupMembership) {
        String randomUUID = UUID.randomUUID().toString();
        log.debug("Starting updateAzureWithMembership function {}.", randomUUID);

        if (resourceGroupMembership.isEmpty()) {
            entraClient.deleteGroupMembership(null, kafkakKey);
        }
        else
        {
            entraClient.addGroupMembership(resourceGroupMembership.get(), kafkakKey);
        }
        log.debug("Stopping updateAzureWithMembership function {}.", randomUUID);
    }
}

