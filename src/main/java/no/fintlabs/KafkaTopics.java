package no.fintlabs;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

//TODO: Remove class
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "fint.kafka.topic.resources")
public class KafkaTopics {
    private String resourcegrouptopic;
    private String resourcegroupmembertopic;

}
