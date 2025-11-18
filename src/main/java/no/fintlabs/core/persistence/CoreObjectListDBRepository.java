package no.fintlabs.core.persistence;

import no.fintlabs.core.CoreObjectList;
import no.fintlabs.core.entity.CoreObject;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CoreObjectListDBRepository<I, T extends CoreObject>
        extends ReactiveCrudRepository<CoreObjectList<I, T>, I> {
}