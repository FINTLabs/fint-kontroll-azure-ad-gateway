package no.fintlabs.kafka;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ResourceGroup
{
    public String id;
    public String resourceId;
    public String displayName;
    public String identityProviderGroupObjectId;
    public String resourceName;
    public String resourceType;
    public String resourceLimit;
}
