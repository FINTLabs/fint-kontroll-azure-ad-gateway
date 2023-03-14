package no.fintlabs;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.microsoft.graph.models.Group;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AzureGroup extends BaseObject {
    private String name;
    public AzureGroup(Group group) {
        this.id = group.id;
        this.name = group.displayName;
    }
}