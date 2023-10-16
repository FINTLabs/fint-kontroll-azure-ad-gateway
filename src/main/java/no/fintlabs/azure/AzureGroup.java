package no.fintlabs.azure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.ExtensionCollectionPage;

import java.lang.reflect.Field;
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
    protected List assignedLabels;

    protected String fintKontrollId;
    protected ExtensionCollectionPage extensions;

    public AzureGroup(Group group, ConfigGroup configGroup) {

        this.id = group.id;
        this.displayName = group.displayName;
        this.members = new ArrayList<>();
        this.assignedLabels = group.assignedLabels;
        this.extensions = group.extensions;
        this.fintKontrollId = group.additionalDataManager().get(configGroup.getFintkontrollidattribute()).getAsString();


    }
}


