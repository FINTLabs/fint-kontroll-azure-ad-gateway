package no.fintlabs.core.persistence;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.CoreObjectEvent;
import no.fintlabs.core.CoreObjectEventType;
import no.fintlabs.core.CoreObjectListOrchestrator;
import no.fintlabs.core.CoreObjectListReactive;
import no.fintlabs.core.entity.*;
import reactor.core.publisher.Mono;

import java.time.Duration;

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

        final int CORES = Runtime.getRuntime().availableProcessors();
        final int CONCURRENCY = Math.min(CORES * 8, 256);
        int waitForPageInSeconds = 10;

/*        orchestrator.getUsers().updates()
                .flatMap(event ->
                        dbRepository.save(event.getObject()) // Save to DB
                                .then(graphPersistenceService.update(event.getEntity())) // When DB succeeds, update MS Graph
                )
                .subscribe(
                        success -> System.out.println("Update processed successfully"),
                        error -> System.err.println("Error processing update: " + error)
                );*/


        // Initialize USER persistence
        orchestrator.getUsers().updates()
                .doOnNext(u -> log.debug("Received: " + u))
                .doOnComplete(() -> log.debug("Upstream completed"))
                .groupBy(CoreObjectEvent::getType)
                .flatMapSequential(groupedFlux -> {
                    if (groupedFlux.key() == CoreObjectEventType.DELETED) {
                        // Special handling for DELETE events
                        return groupedFlux
                                .doOnNext(event -> log.info("Handling DELETE event: {}", event))
                                .flatMap(event -> dbRepository.delete(event), CONCURRENCY);
                    } else {
                        return groupedFlux
                                .windowTimeout(100, Duration.ofSeconds(waitForPageInSeconds))
                                .doOnNext(w -> log.info("New window created"))
                                .flatMapSequential(window ->
                                                window.collectList()
                                                        .filter(batch -> !batch.isEmpty())
                                                        .flatMapMany(batch -> {
                                                            log.info("Processing batch with size " + batch.size());
                                                            return dbRepository.saveAll(batch);
                                                            /*batch.forEach(item -> {
                                                                log.info("  -> processed " + item);
                                                            });*/
                                                            //return Mono.empty();
                                                        }),
                                        CONCURRENCY,
                                        1024
                                );
                    }
                })
                .onErrorContinue((e, o) -> log.info("Failed to update Azure. " + e))
                .doOnComplete(() -> log.info("✅ All batches processed"))
                .subscribe();
    }

    @SuppressWarnings("unchecked")
    private <I, T extends CoreObject> void subscribeToList(
            String key,
            CoreObjectListReactive<I, T> reactiveList,
            CoreObjectListDBRepositoryImpl repository
    ) {
        /*reactiveList.updates()
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
                });*/
    }
}
