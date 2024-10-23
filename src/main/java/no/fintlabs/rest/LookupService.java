package no.fintlabs.rest;

import com.microsoft.graph.models.Group;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.getmembergroups.GetMemberGroupsPostRequestBody;
import com.microsoft.kiota.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.ConfigGroup;
import no.fintlabs.ConfigUser;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.azure.AzureUser;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class LookupService {
    protected final ConfigUser configUser;
    protected final ConfigGroup configGroup;
    protected final GraphServiceClient graphServiceClient;

    public UserWithGroupsDto getAzureUserWithGroups(String userId) {
        try {
            AzureUser azureUser = new AzureUser(graphServiceClient.users().byUserId(userId).get(), configUser);

            GetMemberGroupsPostRequestBody requestBody = new GetMemberGroupsPostRequestBody();
            requestBody.setSecurityEnabledOnly(true);
            List<String> groupIds = graphServiceClient.users().byUserId(userId).getMemberGroups().post(requestBody).getValue();

            String[] selectionCriteriaGroup = new String[]{String.format("id,displayName,%s", configGroup.getFintkontrollidattribute())};
            List<AzureGroup> azureGroups = new ArrayList<>();
            for (String groupId : groupIds) {
                Group group = graphServiceClient
                        .groups()
                        .byGroupId(groupId)
                        .get(requestConfiguration -> {
                                    requestConfiguration.queryParameters.select = selectionCriteriaGroup; });

                if (group.getDisplayName() != null && group.getDisplayName().endsWith(configGroup.getSuffix())
                        && (!group.getAdditionalData().isEmpty()
                        && group.getAdditionalData().containsKey(configGroup.getFintkontrollidattribute()))) {

                    AzureGroup azureGroup = new AzureGroup(group, configGroup);
                    azureGroups.add(azureGroup);
                }
            }
            log.info("*** <<< Rest service found userId {}. User is member of {} FINT kontroll groups >>> ***",
                    userId,
                    azureGroups.size());

            return UserWithGroupsDto.UserWithGroups(azureUser, azureGroups);
        } catch (ApiException ex) {
            log.error("Failed to fetch user or groups: {}", ex.getMessage());
            return new UserWithGroupsDto();
        }
    }
}
