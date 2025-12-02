package no.fintlabs.core.persistence;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.CoreObjectEvent;
import no.fintlabs.core.CoreObjectEventType;
import no.fintlabs.core.CoreObjectList;
import no.fintlabs.core.CoreObjectListReactive;
import no.fintlabs.core.entity.CoreObject;
import no.fintlabs.core.entity.CoreUser;
import org.springframework.data.relational.core.conversion.SaveBatchingAggregateChange;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Repository
@Slf4j
public abstract class CoreObjectListDBRepositoryImpl implements CoreObjectListRepository {

    /*public Flux<CoreObject> processAll(CoreObjectEventType eventType, List<CoreObjectEvent<UUID, CoreUser>> batch) {
        switch(eventType) {
            case CREATED,UPDATED -> { return saveAll(batch);}
            case DELETED -> {return deleteAll(batch);}
        }
    }*/

    @Override
    public Mono<CoreObject> save(Object entity) {
        log.debug("Saving " + entity.getClass().getSimpleName());
        return null;
    }
}
