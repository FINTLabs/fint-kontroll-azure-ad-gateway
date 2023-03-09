package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
@Setter
@Getter
public class AzureUserProducerService {
    private final EntityProducerFactory entityProducerFactory;
    private final EntityProducer<AzureUser> entityProducer;
    //private final EntityTopicNameParameters entityTopicNameParameters;
}
