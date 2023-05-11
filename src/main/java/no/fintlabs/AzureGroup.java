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
        //this.members = group.members;
        this.members = groupMembers(group);
        this.assignedLabels = group.assignedLabels;
    }

    private List<String> groupMembers (Group group)
    {
        List<String> userPrincipalNames = new ArrayList<>();
        DirectoryObjectCollectionPage groupMembers = group.members;
        if (groupMembers != null && groupMembers.getCurrentPage() != null) {
            List<DirectoryObject> members = groupMembers.getCurrentPage();

            for (DirectoryObject member : members) {
                if (member instanceof User) {
                    User user = (User) member;
                    userPrincipalNames.add(user.userPrincipalName);
                }
            }
        }

        return userPrincipalNames;

//                        for (Field field : fields) {
//                                // make the field accessible to be able to read its value
//                                field.setAccessible(true);
//                                try {
//                                        // get the value of the field from the group object
//                                        Object value = field.get(user);
//
//                                        // add the value to the results map if it's not null
//                                        if(field.getName().endsWith(attributeName))
//                                                if (value != null) {
//                                                        return value.toString();
//                                                }
//                                } catch (IllegalAccessException e) {
//                                        // handle the exception if the field is not accessible
//                                        e.printStackTrace();
//                                }
//                        }
//        return null;
    }

}


