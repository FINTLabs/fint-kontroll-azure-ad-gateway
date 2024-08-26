package no.fintlabs.azure;

import com.microsoft.graph.requests.ExtensionCollectionPage;

import java.util.ArrayList;
import java.util.List;
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
    //protected List<String> members;
    protected Long resourceGroupID;

    protected ExtensionCollectionPage extensions;
    public AzureGroup(Group group, ConfigGroup configGroup) {

        this.id = group.id;
        this.displayName = group.displayName;
        //this.members = new ArrayList<>();

        //TODO: Implement tests to verify ResourceID as LONG from kafka [FKS-216]
        if (!group.additionalDataManager().isEmpty() && group.additionalDataManager().containsKey(configGroup.getFintkontrollidattribute()))
        {
            try {
                this.resourceGroupID = Long.valueOf(group.additionalDataManager().get(configGroup.getFintkontrollidattribute()).getAsString());
            } catch (NumberFormatException e) {
                log.warn("Error converting value {} to long", e.getMessage());
                throw e;
            }
        }
    }
}


