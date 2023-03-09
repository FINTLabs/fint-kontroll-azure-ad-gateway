package no.fintlabs;

import com.microsoft.graph.models.Group;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AzureGroup {
    private String id;
    private String name;
    public AzureGroup(Group group) {
        this.id = group.id;
        this.name = group.displayName;
    }
}
