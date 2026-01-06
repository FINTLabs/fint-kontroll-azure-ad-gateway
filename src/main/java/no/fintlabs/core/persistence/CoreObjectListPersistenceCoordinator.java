package no.fintlabs.core.persistence;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azure.HashKey;
import no.fintlabs.core.CoreObjectEvent;
import no.fintlabs.core.CoreObjectEventType;
import no.fintlabs.core.CoreObjectListOrchestrator;
import no.fintlabs.core.CoreObjectListReactive;
import no.fintlabs.core.entity.*;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Accept batches or events from orchestrator.
 *  Persist to DB first.
 *  Trigger Entra ID persistence (or other systems) after DB success.
 *  Handle errors, retries, and fallback logic.
 */
//@AllArgsConstructor
@Slf4j
@Getter
@Setter
public class CoreObjectListPersistenceCoordinator {

    private final int concurrency;
    private final int prefetch;
    private final int batchSize;
    private final Duration batchMaxWait;
    private final CoreObjectListDbOperations<UUID, CoreObject> dbRepository;
    private final MSGraphPersistenceService msGraphPersistenceService;
    private final CoreObjectListOrchestrator orchestrator;

    public CoreObjectListPersistenceCoordinator(
            CoreObjectListDbOperations<UUID, CoreObject> dbRepository,
            MSGraphPersistenceService msGraphPersistenceService,
            CoreObjectListOrchestrator orchestrator
    ) {
        this.dbRepository = dbRepository;
        this.msGraphPersistenceService = msGraphPersistenceService;
        this.orchestrator = orchestrator;

        int cores = Runtime.getRuntime().availableProcessors();
        this.concurrency = Math.min(cores * 8, 256);
        this.prefetch = 1024;
        this.batchSize = 100;
        this.batchMaxWait = Duration.ofSeconds(10);
    }

    private <I, T extends CoreObject> Disposable subscribe(
            String name,
            CoreObjectListReactive<I, T> list,
            ReactiveCrudRepository<T, I> repo
    ) {
        return list.updates()
                .doOnSubscribe(s -> log.info("Starting {}", name))
                .doOnNext(e -> log.debug("[{}] {}", name, e))
                .doOnError(e -> log.error("{} crashed", name, e))
                .doOnComplete(() -> log.info("{} completed", name))

                // Gruppér i praksis på "delete vs upsert", ikke per event-type
                .groupBy(ev -> ev.getType() == CoreObjectEventType.DELETED)
                .flatMap(group -> {
                    boolean isDeleteGroup = group.key();
                    if (isDeleteGroup) {
                        return handleDeletes(name, group, repo);
                    } else {
                        return handleUpserts(name, group, repo);
                    }
                })

                .subscribe();
    }


    private <I, T extends CoreObject> Flux<?> handleDeletes(
            String name,
            Flux<CoreObjectEvent<I, T>> deletes,
            ReactiveCrudRepository<T, I> repo
    ) {
        return deletes.flatMapSequential(ev ->
                        repo.deleteById(ev.getId())
                                .onErrorResume(ex -> {
                                    log.warn("[{}] Delete failed for {}", name, ev, ex);
                                    return Mono.empty();
                                }),
                concurrency,
                prefetch
        );
    }

    private <I, T extends CoreObject> Flux<?> handleUpserts(
            String name,
            Flux<CoreObjectEvent<I, T>> upserts,
            ReactiveCrudRepository<T, I> repo
    ) {
        return upserts
                .map(CoreObjectEvent::getObject)
                .windowTimeout(batchSize, batchMaxWait)
                .flatMapSequential(window ->
                                window.collectList()
                                        .filter(list -> !list.isEmpty())
                                        .flatMapMany(batch -> saveBatch(name, repo, batch)),
                        concurrency,
                        prefetch
                );
    }

    private <I, T extends CoreObject> Flux<T> saveBatch(
            String name,
            ReactiveCrudRepository<T, I> repo,
            List<T> batch
    ) {
        log.info("[{}] Saving batch size={}", name, batch.size());
        return repo.saveAll(batch)
                .onErrorResume(ex -> {
                    log.warn("[{}] SaveAll failed batch size={}", name, batch.size(), ex);
                    return Flux.empty();
                });

    }
}
