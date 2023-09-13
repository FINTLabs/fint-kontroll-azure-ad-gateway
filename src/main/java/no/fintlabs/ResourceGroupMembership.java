package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ResourceGroupMembership
{
    public String id;
    public String resourceRef;
    public String userRef;
    public String roleRef;
}