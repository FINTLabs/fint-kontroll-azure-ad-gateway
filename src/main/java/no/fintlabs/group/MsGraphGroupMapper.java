package no.fintlabs.group;

import com.microsoft.graph.models.Group;
import no.fintlabs.config.Config;
import no.fintlabs.config.ConfigGroup;
import no.fintlabs.kafka.ResourceGroup;

public class MsGraphGroupMapper {

    public static String getDisplayname(ResourceGroup resourceGroup, ConfigGroup configGroup) {
        return configGroup.getPrefix().toLowerCase() +
                resourceGroup.getResourceType().substring(0, 3) +
                "-" +
                resourceGroup.getResourceName().replace("\s", ".") +
                configGroup.getSuffix().toLowerCase();
    }

    public static Group toMsGraphGroup(ResourceGroup resourceGroup, ConfigGroup configGroup, Config config) {
        Group group = new Group();
        int groupMailEnabledMaxLen = 64;

        group.setDisplayName(getDisplayname(resourceGroup, configGroup));

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
