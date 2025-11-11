package no.fintlabs.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.fintlabs.azure.HashKey;
import no.fintlabs.core.entity.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
@Service
public class CoreObjectListOrchestrator {
    private CoreObjectList<String, CoreDelta> delta = new CoreObjectList<>();
    private CoreObjectList<UUID, CoreUser> users = new CoreObjectList<>();
    private CoreObjectList<UUID, CoreUser> usersExternal = new CoreObjectList<>();
    private CoreObjectList<Long, CoreGroup> groups = new CoreObjectList<>();
    private CoreObjectList<HashKey, CoreMembership> memberships = new CoreObjectList<>();

    public void clear() {
        users.clear();
        memberships.clear();
    }

    public void clearUsers() {
        users.clear();
        usersExternal.clear();
    }
}