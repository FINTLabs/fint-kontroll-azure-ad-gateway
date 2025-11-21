package no.fintlabs.core.persistence;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.CoreObjectListOrchestrator;
import no.fintlabs.core.CoreObjectListReactive;
import no.fintlabs.core.entity.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/** Accept batches or events from orchestrator.
 *  Persist to DB first.
 *  Trigger Entra ID persistence (or other systems) after DB success.
 *  Handle errors, retries, and fallback logic.
 */
//@AllArgsConstructor
@Slf4j
public class CoreObjectListPersistenceCoordinator {

    /*private final CoreObjectListDBRepository dbRepository;
    private final MSGraphPersistenceService graphPersistenceService;
    private final CoreObjectListOrchestrator orchestrator;*/

    public CoreObjectListPersistenceCoordinator(
            CoreObjectListDBRepositoryImpl dbRepository,
            MSGraphPersistenceService graphPersistenceService,
            CoreObjectListOrchestrator orchestrator) {
        orchestrator.getAllReactiveLists().forEach((key, reactiveList) -> {
            subscribeToList(key, reactiveList, dbRepository);
        });
    }

    @SuppressWarnings("unchecked")
    private <I, T extends CoreObject> void subscribeToList(
            String key,
            CoreObjectListReactive<I, T> reactiveList,
            CoreObjectListDBRepositoryImpl repository
    ) {
        reactiveList.updates()
                .map(event -> event.getObject()) // CoreObject<T>
                .bufferTimeout(50, Duration.ofSeconds(5))
                .flatMap(batch -> {
                    Class<T> type = batch.get(0).getClass(); // assuming homogeneous batch
                    CoreObjectListRepository<T> repo = registry.getRepository(type);
                    Mono<Void> mainSave = repo.saveBatch(Flux.fromIterable(batch));

                    List<CoreObjectListRepository<?>> dependents = registry.getDependentRepositories(type);
                    List<Mono<Void>> dependentSaves = dependents.stream()
                            .map(depRepo -> depRepo.saveBatch(Flux.fromIterable(batch)))
                            .toList();

                    return Mono.when(mainSave, Mono.when(dependentSaves));
                });
    }
}
