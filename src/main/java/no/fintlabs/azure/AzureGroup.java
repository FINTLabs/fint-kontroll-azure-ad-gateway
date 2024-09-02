package no.fintlabs.azure;

import com.microsoft.graph.models.Group;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.ConfigGroup;

@Setter
@Getter
@Slf4j
public class AzureGroup {

    protected String id;
    protected String displayName;
    protected Long resourceGroupID;

    public AzureGroup(Group group, ConfigGroup configGroup) {

        this.id = group.getId();
        this.displayName = group.getDisplayName();

        //TODO: Implement tests to verify ResourceID as LONG from kafka [FKS-216]
        if (!group.getAdditionalData().isEmpty() && group.getAdditionalData().containsKey(configGroup.getFintkontrollidattribute()))
        {
            try {
                this.resourceGroupID = Long.valueOf(group.getAdditionalData().get(configGroup.getFintkontrollidattribute()).toString());
            } catch (NumberFormatException e) {
                log.warn("Error converting value {} to long", e.getMessage());
                throw e;
            }
        }
    }
}


