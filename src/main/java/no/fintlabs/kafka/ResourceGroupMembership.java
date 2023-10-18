package no.fintlabs.kafka;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ResourceGroupMembership
{
    public String id;
    public String azureGroupRef;
    public String azureUserRef;
    public String roleRef;
}
