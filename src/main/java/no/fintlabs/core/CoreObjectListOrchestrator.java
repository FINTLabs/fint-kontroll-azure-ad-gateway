package no.fintlabs.core;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azure.HashKey;
import no.fintlabs.core.entity.*;
import no.fintlabs.core.persistence.CoreObjectListPersistenceCoordinator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Service
@Slf4j
public class CoreObjectListOrchestrator {

    private final Map<String, CoreObjectListReactive<?,?>> reactiveLists = new HashMap<>();

    public CoreObjectListOrchestrator() {
        reactiveLists.put("users", new CoreObjectListReactive<UUID, CoreUser>());
        reactiveLists.put("usersexternal", new CoreObjectListReactive<UUID, CoreUser>());
        reactiveLists.put("device", new CoreObjectListReactive<UUID, CoreDevice>());
        reactiveLists.put("groups", new CoreObjectListReactive<Long, CoreGroup>());
        reactiveLists.put("memberships", new CoreObjectListReactive<HashKey, CoreMembership>());
        reactiveLists.put("delta", new CoreObjectListReactive<String, CoreDelta>());
    }

    public Map<String, CoreObjectListReactive<?,?>> getAllReactiveLists() {
        return reactiveLists;
    }

    /*private final CoreObjectListReactive<String, CoreDelta> delta = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<UUID, CoreUser> users = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<UUID, CoreUser> usersExternal = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<Long, CoreGroup> groups = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<HashKey, CoreMembership> memberships = new CoreObjectListReactive<>();*/

    @PostConstruct
    public void init() {
            for (Map.Entry<String, CoreObjectListReactive<?, ?>> entry : reactiveLists.entrySet()) {
                entry.getValue().updates()
                        .subscribe(userEvent -> log.debug(userEvent.getId().toString()));
            }
    }

    public void clear() {
        reactiveLists.get("users").clear();
        reactiveLists.get("memberships").clear();
    }

    public void clearUsers() {
        reactiveLists.get("users").clear();
        reactiveLists.get("usersexternal").clear();
    }

    /*public void process(Flux<List<CoreObjectEvent>> batches) {
        batches.flatMap(persistenceCoordinator::persistBatch)
                .subscribe(
                        null,
                        error -> System.err.println("Error in orchestration: " + error),
                        () -> System.out.println("✅ All batches processed")
                );
    }*/
}