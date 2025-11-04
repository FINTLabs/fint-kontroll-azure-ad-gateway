package no.fintlabs.kafka;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;

@Builder(toBuilder = true)
@Getter
@Slf4j
@Data
public class ResourceGroup implements Serializable
{
    @Serial
    private final String id;
    @Serial
    private final String resourceId;
    @Serial
    private final String displayName;
    @Serial
    private final String identityProviderGroupObjectId;
    @Serial
    private final String resourceName;
    @Serial
    private final String resourceType;
    @Serial
    private final String resourceLimit;
}
