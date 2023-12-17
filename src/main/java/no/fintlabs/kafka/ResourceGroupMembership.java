package no.fintlabs.kafka;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Builder(toBuilder = true)
@Getter
@Slf4j
@EqualsAndHashCode

public class ResourceGroupMembership
{
    private String id;
    @NonNull
    private String azureGroupRef;
    @NonNull
    private String azureUserRef;

    private String roleRef;
}
