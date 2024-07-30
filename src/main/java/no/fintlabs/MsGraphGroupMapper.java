package no.fintlabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.Group;
import no.fintlabs.kafka.ResourceGroup;

public class MsGraphGroupMapper {

    public Group toMsGraphGroup(ResourceGroup resourceGroup, ConfigGroup configGroup, Config config) {
        Group group = new Group();

        int groupMailEnabledMaxLen = 64;

        //TODO: Change to new functions on Change of Graph to 6.*.* [FKS-883]
        group.displayName = (configGroup.getPrefix() +
                             resourceGroup.getResourceType().substring(0, 3) +
                             "-" +
                             resourceGroup.getResourceName().replace("\s", ".") +
                             configGroup.getSuffix()).toLowerCase();

        group.mailEnabled = false;
        group.securityEnabled = true;

        // Remove special characters
        String mailNickname = resourceGroup.getResourceName()
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();

        // Make length max [groupMailEnabledMaxLen] long
        if (mailNickname.length() > groupMailEnabledMaxLen) {
            mailNickname = mailNickname.substring(0, groupMailEnabledMaxLen);
        }
        group.mailNickname = mailNickname;

        group.additionalDataManager().put(configGroup.getFintkontrollidattribute(), new JsonPrimitive(resourceGroup.getId()));
        group.additionalDataManager().put("uniqueName", new JsonPrimitive(resourceGroup.getId()));

        // Removed from general converter, and moved to function "addGroupToAzure"

//        String owner = "https://graph.microsoft.com/v1.0/directoryObjects/" + config.getEntobjectid();
//        var owners = new JsonArray();
//        owners.add(owner);
//        group.additionalDataManager().put("owners@odata.bind",  owners);

        return group;
    }
}
