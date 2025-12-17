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

//    private final Disposable usersSub;
//    private final Disposable usersExternalSub;
//    private final Disposable devicesSub;
//    private final Disposable groupsSub;
//    private final Disposable membershipsSub;
//    private final Disposable deltaSub;

    public CoreObjectListPersistenceCoordinator(
            // reactive lists (kildene)
            CoreObjectListDBRepositoryImpl dbRepository,
            MSGraphPersistenceService msGraphPersistenceService,
            CoreObjectListOrchestrator orchestrator
//            ,
//            CoreObjectListReactive<UUID, CoreUser> users,
//            CoreObjectListReactive<UUID, CoreUser> usersExternal,
//            CoreObjectListReactive<UUID, CoreDevice> devices,
//            CoreObjectListReactive<Long, CoreGroup> groups,
//            CoreObjectListReactive<HashKey, CoreMembership> memberships,
//            CoreObjectListReactive<String, CoreDelta> delta,

            // typed Spring Data repos (målene)
//            ReactiveCrudRepository<CoreUser, UUID> userRepo,
//            ReactiveCrudRepository<CoreUser, UUID> usersExternalRepo,
//            ReactiveCrudRepository<CoreDevice, UUID> devicesRepo,
//            ReactiveCrudRepository<CoreGroup, Long> groupsRepo,
//            ReactiveCrudRepository<CoreMembership, HashKey> membershipsRepo,
//            ReactiveCrudRepository<CoreDelta, String> deltaRepo
    ) {
        int cores = Runtime.getRuntime().availableProcessors();
        this.concurrency = Math.min(cores * 8, 256);
        this.prefetch = 1024;
        this.batchSize = 100;
        this.batchMaxWait = Duration.ofSeconds(10);

//        this.usersSub = subscribe("users", users, usersRepo);
//        this.usersExternalSub = subscribe("usersExternal", usersExternal, usersExternalRepo);
//        this.devicesSub = subscribe("devices", devices, devicesRepo);
//        this.groupsSub = subscribe("groups", groups, groupsRepo);
//        this.membershipsSub = subscribe("memberships", memberships, membershipsRepo);
//        this.deltaSub = subscribe("delta", delta, deltaRepo);
    }

//    public void stop() {
//        usersSub.dispose();
//        usersExternalSub.dispose();
//        devicesSub.dispose();
//        groupsSub.dispose();
//        membershipsSub.dispose();
//        deltaSub.dispose();
//    }

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

                .groupBy(CoreObjectEvent::getType)
                .flatMap(group -> {
                    if (group.key() == CoreObjectEventType.DELETED) {
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
                        repo.deleteById(ev.getId())   // <-- viktig: deleteById, ikke delete(T)
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
                .map(CoreObjectEvent::getObject) // T
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
