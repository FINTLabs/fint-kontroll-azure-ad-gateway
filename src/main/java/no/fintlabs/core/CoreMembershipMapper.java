package no.fintlabs.core;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azure.AzureGroupMembership;
import no.fintlabs.azure.HashKey;
import no.fintlabs.core.entity.CoreGroup;
import no.fintlabs.core.entity.CoreMembership;
import no.fintlabs.core.entity.CoreUser;
import reactor.util.function.Tuples;

import java.util.UUID;

@Slf4j
public class CoreMembershipMapper {
    static public CoreMembership toDBMembership (
            AzureGroupMembership membership,
            CoreObjectList<UUID, CoreUser> users,
            CoreObjectList<UUID, CoreGroup> groups) {
        try {
            return new CoreMembership(
                    HashKey.createHashKey(membership),
                    users.get(UUID.fromString(membership.getUser_id())),
                    groups.get(UUID.fromString(membership.getGroup_id())));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
    static public CoreMembership toDBMembership (
            UUID user_id,
            UUID group_id,
            CoreObjectList<UUID, CoreUser> users,
            CoreObjectList<UUID, CoreGroup> groups) {
        try {
            return new CoreMembership(
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