package no.fintlabs.kafka;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;

@Builder(toBuilder = true)
@Getter
@Slf4j
@Data
@AllArgsConstructor
public class ResourceGroup
{
    private final String id;
    private final String displayName;
    private final String identityProviderGroupObjectId;
    private final String resourceName;
    private final String resourceType;
}
