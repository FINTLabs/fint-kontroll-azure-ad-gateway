package no.fintlabs.kafka;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.Group;
import jakarta.annotation.Resource;
import lombok.Builder;
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

@Builder
@RequiredArgsConstructor
@Slf4j
public class ResourceGroup
{
    public final String id;
    public final String resourceId;
    public final String displayName;
    public final String identityProviderGroupObjectId;
    public final String resourceName;
    public final String resourceType;
    public final String resourceLimit;
}
