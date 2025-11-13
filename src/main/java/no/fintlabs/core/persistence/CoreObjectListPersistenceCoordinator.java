package no.fintlabs.core.persistence;

import no.fintlabs.core.CoreObjectEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Accept batches or events from orchestrator.
 *  Persist to DB first.
 *  Trigger Entra ID persistence (or other systems) after DB success.
 *  Handle errors, retries, and fallback logic.
 */
public class CoreObjectListPersistenceCoordinator {

    private CoreObjectListDBRepository dbRepository;
    private MSGraphPersistenceService graphPersistenceService;

    public Mono<Void> persistBatch(List<CoreObjectEvent> batch) {
        return Flux.fromIterable(batch)
                .map(this::toEntity)
                .collectList()
                .flatMap(dbRepository::saveAll)
                .then(graphPersistenceService.persist(batch))
                .onErrorResume(e -> {
                    // Log, retry, or send to DLQ
                    System.err.println("Persistence failed: " + e.getMessage());
                    return Mono.empty();
                });
    }

    private CoreObjectListEntity toEntity(CoreObjectEvent event) {
        return new CoreObjectListEntity(event.getId(), event.getType(), event.getPayload());
    }
}
