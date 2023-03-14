package no.fintlabs;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import okhttp3.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Slf4j
@AllArgsConstructor
public class ProducerService<T extends BaseObject> {
    /*private final EntityProducerFactory entityProducerFactory;
    //private EntityProducer<T> entityProducer;
    private final EntityTopicNameParameters entityTopicNameParameters;
    private final EntityTopicService entityTopicService;*/

    @PostConstruct
    public void initVars () {
/*        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource(entityProducer.getClass().getName())
                .build();*/
        //entityTopicService.ensureTopic(entityTopicNameParameters,0);
    }


    /*public ProducerService (
            EntityTopicService entityTopicService,
            EntityProducerFactory entityProducerFactory,
            Class<T> valueClass) {

        entityProducer = entityProducerFactory.createProducer(valueClass);
        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource(valueClass.getName())
                .build();
        entityTopicService.ensureTopic(entityTopicNameParameters,0);
    }*/
    public void publish(T object) {
        /*entityProducer.send(
                EntityProducerRecord.<T>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(object.getId())
                        .value(object)
                        .build()
        );*/
    }
}
