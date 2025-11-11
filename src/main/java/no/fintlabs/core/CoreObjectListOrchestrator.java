package no.fintlabs.core;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azure.HashKey;
import no.fintlabs.core.entity.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
@Service
@Slf4j
public class CoreObjectListOrchestrator {
    private final CoreObjectListReactive<String, CoreDelta> delta = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<UUID, CoreUser> users = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<UUID, CoreUser> usersExternal = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<Long, CoreGroup> groups = new CoreObjectListReactive<>();
    private final CoreObjectListReactive<HashKey, CoreMembership> memberships = new CoreObjectListReactive<>();

    @PostConstruct
    public void init() {
        users.updates()
                .subscribe(userEvent -> log.debug(userEvent.getId().toString()));
    }

    public void clear() {
        users.clear();
        memberships.clear();
    }

    public void clearUsers() {
        users.clear();
        usersExternal.clear();
    }

}