package no.fintlabs.rest;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.users.item.getmembergroups.GetMemberGroupsPostResponse;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.azure.AzureUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Data
public class UserWithGroupsDto {
    private AzureUser user;
    private List<AzureGroup> groups;

    public static UserWithGroupsDto UserWithGroups(AzureUser user, List<AzureGroup> groups) {
        UserWithGroupsDto dto = new UserWithGroupsDto();
        dto.setUser(user);
        dto.setGroups(groups);
        return dto;
    }

}