package no.fintlabs.azure;

import com.microsoft.graph.requests.ExtensionCollectionPage;

import java.util.ArrayList;
import java.util.List;
import com.microsoft.graph.models.Group;
import lombok.Getter;
import lombok.Setter;
import no.fintlabs.ConfigGroup;

@Setter
@Getter
public class AzureGroup {


    protected String id;
    protected String displayName;
    protected List<String> members;

    protected long fintKontrollRoleId;
    protected ExtensionCollectionPage extensions;

    public AzureGroup(Group group, ConfigGroup configGroup) {

        this.id = group.id;
        this.displayName = group.displayName;
        this.members = new ArrayList<>();

        //this.extensions = group.extensions;
        //TODO: Get resource ID on Azure object
        if (!group.additionalDataManager().isEmpty() && group.additionalDataManager().containsKey(configGroup.getFintkontrollidattribute()))
        {
            this.fintKontrollRoleId = group.additionalDataManager().get(configGroup.getFintkontrollidattribute()).getAsLong();
        }


    }
}


