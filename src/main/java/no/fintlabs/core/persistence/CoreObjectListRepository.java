package no.fintlabs.core.persistence;

import no.fintlabs.core.CoreObjectList;
import no.fintlabs.core.entity.CoreObject;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public abstract interface CoreObjectListRepository<I, T extends CoreObject> extends ReactiveCrudRepository<CoreObjectList<I, T>, I> {
    Mono<T> save(T entity);
    //Mono<Void> saveBatch(CoreObjectList<I, T> objects);
};

/*
public interface CoreObjectListDBRepository<I, T extends CoreObject>
        extends ReactiveCrudRepository<CoreObjectList<I, T>, I> {
}
 */