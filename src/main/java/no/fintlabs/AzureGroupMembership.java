package no.fintlabs;

import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.GroupMembers;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureGroupMembership extends BaseObject {
    private String user_id;
    private String group_id;

    public AzureGroupMembership(String group_id, DirectoryObject directoryObject) {
        this.group_id = group_id;
        this.user_id = directoryObject.id;
    }
    /*public AzureGroupMembership(T membership) {
        this.id = membership.groupId;
        //this.user_id = group.
    }*/
}