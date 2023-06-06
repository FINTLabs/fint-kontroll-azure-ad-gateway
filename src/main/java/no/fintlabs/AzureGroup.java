package no.fintlabs;

import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.options.QueryOption;

import java.util.ArrayList;
import java.util.List;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.DirectoryObjectCollectionPage;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter

public class AzureGroup extends BaseObject {

    protected String id;
    protected String displayName;
    protected List<String> members;
    protected List assignedLabels;

    public AzureGroup(Group group) {
        this.id = group.id;
        this.displayName = group.displayName;
        this.members = new ArrayList<>();
        this.assignedLabels = group.assignedLabels;
    }
}


