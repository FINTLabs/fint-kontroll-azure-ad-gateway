package no.fintlabs.core.persistence;
import lombok.AllArgsConstructor;
import no.fintlabs.core.CoreObjectList;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Repository
public class CoreObjectListDBRepositoryImpl implements CoreObjectListRepository {
    private final CoreObjectListDBRepository delegate;

    @Override
    public Mono<Void> saveBatch(Flux<CoreObjectList> objects) {
        return delegate.saveAll(objects).then();
    }
}
