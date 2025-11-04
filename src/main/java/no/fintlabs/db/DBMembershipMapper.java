package no.fintlabs.db;

import jakarta.persistence.Tuple;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azure.AzureGroupMembership;
import no.fintlabs.azure.HashKey;
import no.fintlabs.db.entity.DBGroup;
import no.fintlabs.db.entity.DBMembership;
import no.fintlabs.db.entity.DBUser;
import org.apache.commons.lang3.SerializationUtils;
import reactor.util.function.Tuples;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Slf4j
public class DBMembershipMapper {
    static public DBMembership toDBMembership (
            AzureGroupMembership membership,
            DBObjectList<UUID, DBUser> users,
            DBObjectList<UUID, DBGroup> groups) {
        try {
            return new DBMembership(
                    HashKey.createHashKey(membership),
                    users.get(UUID.fromString(membership.getUser_id())),
                    groups.get(UUID.fromString(membership.getGroup_id())));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
    static public DBMembership toDBMembership (
            UUID user_id,
            UUID group_id,
            DBObjectList<UUID, DBUser> users,
            DBObjectList<UUID, DBGroup> groups) {
        try {
            return new DBMembership(
                    HashKey.createHashKey("test123"),
                    users.get(user_id),
                    groups.get(group_id)
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
    static public HashKey toDBMembershipHashKey (UUID user_id, UUID group_id) {
        return HashKey.createHashKey(Tuples.of(user_id,group_id));
    }
    static public HashKey toDBMembershipHashKey (AzureGroupMembership membership) {
        return HashKey.createHashKey(Tuples.of(membership.getUser_id(),membership.getGroup_id()));
    }
}