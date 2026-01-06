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


@Slf4j
@Getter
public class CoreObjectListOrchestrator {

    private final Map<String, CoreObjectListReactive<?, ?>> reactiveLists = new HashMap<>();

    private final CoreObjectListReactive<UUID, CoreUser> users = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<UUID, CoreUser> usersExternal = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<UUID, CoreDevice> devices = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<Long, CoreGroup> groups = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<HashKey, CoreMembership> memberships = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<String, CoreDelta> delta = new CoreObjectListReactive<>();

    @PostConstruct
    public void init() {
        reactiveLists.put("users", users);
        reactiveLists.put("usersExternal", usersExternal);
        reactiveLists.put("devices", devices);
        reactiveLists.put("groups", groups);
        reactiveLists.put("memberships", memberships);
        reactiveLists.put("delta", delta);

        log.info("Registered reactive lists: {}", reactiveLists.keySet());
    }

    public void clearAll() {
        reactiveLists.forEach((name, list) -> {
            try {
                list.clear();
                log.debug("Cleared {}", name);
            } catch (Exception e) {
                log.warn("Failed to clear {}", name, e);
            }
        });
    }

    /** Tøm én liste (bruk de samme nøklene som i init()) */
    public boolean clear(String key) {
        CoreObjectListReactive<?, ?> list = reactiveLists.get(key);
        if (list == null) {
            log.warn("No reactive list registered for key='{}'", key);
            return false;
        }
        list.clear();
        return true;
    }

    /** Behold “smarte” helpers om dere vil */
    public void clearUsers() {
        users.clear();
        usersExternal.clear();
    }

    public void clearMemberships() {
        memberships.clear();
    }

    public void clearGroups() {
        groups.clear();
    }

    public void clearDevices() {
        devices.clear();
    }

    public void clearDelta() {
        delta.clear();
    }
}
