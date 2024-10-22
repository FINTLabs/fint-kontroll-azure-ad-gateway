package no.fintlabs.rest;

import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.getmembergroups.GetMemberGroupsPostRequestBody;
import com.microsoft.graph.users.item.getmembergroups.GetMemberGroupsPostResponse;
import com.microsoft.kiota.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.ConfigGroup;
import no.fintlabs.ConfigUser;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class LookupService {
    protected final ConfigUser configUser;
    protected final ConfigGroup configGroup;
    protected final GraphServiceClient graphServiceClient;

    public UserWithGroupsDto getAzureUserWithGroups(String objectId) {
        String[] selectionCriteria = new String[]{String.join(",", configUser.AllAttributes())};
        User user = null;
        GetMemberGroupsPostResponse userGroupMember = null;

        try {
            // Fetch the user information
            user = graphServiceClient.users().byUserId(objectId).get(requestConfiguration -> {
                requestConfiguration.queryParameters.select = selectionCriteria;
            });

            // Fetch the group memberships
            userGroupMember = graphServiceClient.users().byUserId(user.getId()).getMemberGroups().post(new GetMemberGroupsPostRequestBody());

            log.info("*** <<< Found user {} from getAzureUser in API >>> ***", user.getUserPrincipalName());

            // Return both user and groups in the DTO
            return new UserWithGroupsDto(user, userGroupMember.getValue());
        } catch (ApiException ex) {
            log.error("Failed to fetch user or groups: {}", ex.getMessage());
            return null;  // Handle error cases appropriately
        }
    }
}
