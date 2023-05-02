package no.fintlabs;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.DirectoryObjectCollectionPage;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AzureGroup extends BaseObject {
    private DirectoryObjectCollectionPage members;
    private String name;
    public AzureGroup(Group group) {
        this.id = group.id;
        this.name = group.displayName;
        this.members = group.members;
    }
}