package no.fintlabs.rest;
import lombok.Data;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.azure.AzureUser;
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