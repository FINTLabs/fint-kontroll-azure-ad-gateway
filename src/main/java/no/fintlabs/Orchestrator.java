package no.fintlabs;

import lombok.AllArgsConstructor;
import no.fintlabs.kafka.ResourceGroupMembershipDownloader;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


@Component
@AllArgsConstructor

public class Orchestrator {

    private final ResourceGroupMembershipDownloader resourceGroupMembershipDownloader;

    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.fixed-delay-ms}"
    )
    private void pullAllMemberships() {
        resourceGroupMembershipDownloader.callApi();
    }
}

