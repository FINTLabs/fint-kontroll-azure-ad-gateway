package no.fintlabs.db;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.fintlabs.db.entity.DBGroup;
import no.fintlabs.db.entity.DBMembership;
import no.fintlabs.db.entity.DBUser;
import org.springframework.stereotype.Service;

@Getter
@RequiredArgsConstructor
@Service
public class DBObjectListOrchestrator {
    private DBDelta delta;
    private DBObjectList<DBUser> users = new DBObjectList<>();
    private DBObjectList<DBUser> usersExternal = new DBObjectList<>();
    private DBObjectList<DBGroup> groups = new DBObjectList<>();
    private DBObjectList<DBMembership> memberships = new DBObjectList<>();

    public void clear() {
        users.clear();
        memberships.clear();
    }

    public void clearUsers() {
        users.clear();
        usersExternal.clear();
    }
}