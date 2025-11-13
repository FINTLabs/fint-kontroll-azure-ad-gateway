package no.fintlabs.core.persistence;

import no.fintlabs.core.entity.CoreObject;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CoreObjectRepository<T extends CoreObject> {
    Mono<Void> saveBatch(Flux<T> objects);
}
