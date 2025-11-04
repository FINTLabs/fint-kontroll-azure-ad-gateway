package no.fintlabs.db;

import no.fintlabs.azure.AzureUser;
import no.fintlabs.azure.AzureUserExternal;
import no.fintlabs.db.entity.DBUser;

import java.util.UUID;

public class DBUserMapper {
    public static DBUser toDBUser(AzureUser user) {
        return new DBUser(UUID.fromString(user.getIdpUserObjectId()));
    }

    public static DBUser toDBUser(AzureUserExternal user) {
        return new DBUser(UUID.fromString(user.getIdpUserObjectId()));
    }
}
