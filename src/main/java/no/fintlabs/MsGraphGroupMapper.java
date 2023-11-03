package no.fintlabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.Group;
import no.fintlabs.kafka.ResourceGroup;

public class MsGraphGroupMapper {

    public Group toMsGraphGroup(ResourceGroup resourceGroup, ConfigGroup configGroup, Config config) {
        Group group = new Group();

        //String spelExpression = 'configGroup.prefix + resourceType.substring(0, 3).toLowerCase() + "-" + resourceName.replace("\s", ".").toLowerCase() + configGroup.suffix'
        /*ExpressionParser parser = new SpelExpressionParser();
        String spelExpression = "displayName";
        Expression exp = parser.parseExpression(spelExpression);

        EvaluationContext context = new SimpleEvaluationContext.Builder().withRootObject(this).build();
        group.displayName = (String) exp.getValue(context);*/

        //group.displayName = (String) exp.getValue();

        //group.displayName = configGroup.prefix + resourceName + configGroup.suffix;

        // group.displayName = cfgPrefix + resourceGroup.resourceName + cfgSuffix;
        // if (configGroup.aslowercase)
        //    group.displayName = group.displayName.toLowerCase();
        group.displayName = (configGroup.getPrefix() +
                             resourceGroup.resourceType.substring(0, 3) +
                             "-" +
                             resourceGroup.resourceName.replace("\s", ".") +
                             configGroup.getSuffix()).toLowerCase();
        group.mailEnabled = false;
        group.securityEnabled = true;
        group.mailNickname = resourceGroup.resourceName.replaceAll("[^a-zA-Z0-9]", ""); // Remove special characters
        group.additionalDataManager().put(configGroup.getFintkontrollidattribute(), new JsonPrimitive(resourceGroup.id));

        String owner = "https://graph.microsoft.com/v1.0/directoryObjects/" + config.getEntobjectid();
        var owners = new JsonArray();
        owners.add(owner);
        group.additionalDataManager().put("owners@odata.bind",  owners);

        return group;
    }
}
