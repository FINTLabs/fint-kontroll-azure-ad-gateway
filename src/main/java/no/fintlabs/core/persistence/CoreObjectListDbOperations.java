package no.fintlabs.core.persistence;

import no.fintlabs.core.entity.CoreObject;
import reactor.core.publisher.Mono;

public interface CoreObjectListDbOperations<I, T extends CoreObject> {
    Mono<T> save(T entity);
}