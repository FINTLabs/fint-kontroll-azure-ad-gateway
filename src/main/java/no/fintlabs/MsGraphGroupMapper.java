package no.fintlabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.Group;
import no.fintlabs.kafka.ResourceGroup;

import java.util.HashMap;

public class MsGraphGroupMapper {

    public Group toMsGraphGroup(ResourceGroup resourceGroup, ConfigGroup configGroup, Config config) {
        Group group = new Group();
        int groupMailEnabledMaxLen = 64;

        group.setDisplayName(configGroup.getPrefix().toLowerCase() +
                             resourceGroup.getResourceType().substring(0, 3) +
                             "-" +
                             resourceGroup.getResourceName().replace("\s", ".") +
                             configGroup.getSuffix().toLowerCase());

        group.setMailEnabled(false);
        group.setSecurityEnabled(true);

        // Remove special characters
        String mailNickname = resourceGroup.getResourceName()
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();

        // Make length max [groupMailEnabledMaxLen] long
        if (mailNickname.length() > groupMailEnabledMaxLen) {
            mailNickname = mailNickname.substring(0, groupMailEnabledMaxLen);
        }
        group.setMailNickname(mailNickname);

        return group;
    }
}
