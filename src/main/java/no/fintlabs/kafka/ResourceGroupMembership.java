package no.fintlabs.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Builder(toBuilder = true)
@Getter
@Slf4j

public class ResourceGroupMembership
{
    private String id;
    @NonNull
    private String azureGroupRef;
    @NonNull
    private String azureUserRef;
    @NonNull
    private String roleRef;
}
