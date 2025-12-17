package no.fintlabs.core.persistence;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.entity.CoreObject;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class CoreObjectListDBRepositoryImpl<I, T extends CoreObject>
        implements CoreObjectListDbOperations<I, T> {

    @Override
    public Mono<T> save(T entity) {
        log.debug("Saving {}", entity.getClass().getSimpleName());
        return Mono.just(entity); // TODO: ekte lagring
    }
}
