package no.fintlabs.db;

import no.fintlabs.azure.AzureUser;
import no.fintlabs.azure.AzureUserExternal;

import java.util.UUID;

public class DBUserMapper {
    static public DBUser toDBUser(AzureUser user) {
        return new DBUser(UUID.fromString(user.getIdpUserObjectId()));
    }

    static public DBUser toDBUser(AzureUserExternal user) {
        return new DBUser(UUID.fromString(user.getIdpUserObjectId()));
    }
}
