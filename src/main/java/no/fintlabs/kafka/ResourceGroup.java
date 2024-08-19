package no.fintlabs.kafka;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Builder(toBuilder = true)
@Getter
@Slf4j
@Data
public class ResourceGroup
{
    private final String id;
    private final String resourceId;
    private final String displayName;
    private final String identityProviderGroupObjectId;
    private final String resourceName;
    private final String resourceType;
    private final String resourceLimit;
}
