package no.fintlabs.kafka;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.Group;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.Config;
import no.fintlabs.ConfigGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

@RequiredArgsConstructor
@Slf4j
@EnableAutoConfiguration
@Resource

public class ResourceGroup
{
    public final String id;
    public final String resourceId;
    public final String displayName;
    public final String identityProviderGroupObjectId;
    public final String resourceName;
    public final String resourceType;
    public final String resourceLimit;

    @Autowired
    private ConfigGroup configGroup;
    @Autowired
    private Config config;

    public Group toMSGraphGroup() {
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
                resourceType.substring(0, 3) +
                "-" +
                resourceName.replace("\s", ".") +
                configGroup.getSuffix()).toLowerCase();
        group.mailEnabled = false;
        group.securityEnabled = true;
        group.mailNickname = resourceName.replaceAll("[^a-zA-Z0-9]", ""); // Remove special characters
        group.additionalDataManager().put(configGroup.fintkontrollidattribute, new JsonPrimitive(id));

        String owner = "https://graph.microsoft.com/v1.0/directoryObjects/" + config.getEntobjectid();
        var owners = new JsonArray();
        owners.add(owner);
        group.additionalDataManager().put("owners@odata.bind",  owners);

        return group;
    }
}