package no.fintlabs.core.persistence;

import no.fintlabs.core.CoreObjectList;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CoreObjectListDBRepository extends ReactiveCrudRepository<CoreObjectList, String> {
}