package no.fintlabs.db;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.fintlabs.db.entity.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
@Service
public class DBObjectListOrchestrator {
    private DBObjectList<String, DBDelta> delta = new DBObjectList<>();
    private DBObjectList<UUID, DBUser> users = new DBObjectList<>();
    private DBObjectList<UUID, DBUser> usersExternal = new DBObjectList<>();
    private DBObjectList<UUID, DBGroup> groups = new DBObjectList<>();
    private DBObjectList<UUID, DBMembership> memberships = new DBObjectList<>();

    public void clear() {
        users.clear();
        memberships.clear();
    }

    public void clearUsers() {
        users.clear();
        usersExternal.clear();
    }
}