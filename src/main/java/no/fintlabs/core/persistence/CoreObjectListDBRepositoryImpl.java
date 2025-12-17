package no.fintlabs.core.persistence;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.entity.CoreObject;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Slf4j
public abstract class CoreObjectListDBRepositoryImpl<I, T extends CoreObject>
        implements CoreObjectListRepository<I, T> {

    /*@Override
    public Flux<T> saveAll(Iterable<T> entities) {
        return super.saveAll(entities);
    }*/

    @Override
    public Mono<T> save(T entity) {
        log.debug("Saving " + entity.getClass().getSimpleName());
        return null;
    }
}
