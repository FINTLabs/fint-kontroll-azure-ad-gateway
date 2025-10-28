package no.fintlabs.db;

import no.fintlabs.azure.AzureGroupMembership;

import java.util.UUID;

public class DBMembershipMapper {
    static public DBMembership toDBMembership(
            AzureGroupMembership membership,
            DBObjectList<DBUser> users,
            DBObjectList<DBGroup> groups) {
        return new DBMembership(UUID.randomUUID(), users.get(membership.getUser_id()), groups.get(membership.getGroup_id()));
    }
    static public DBMembership toDBMembership(
            String user_id,
            String group_id,
            DBObjectList<DBUser> users,
            DBObjectList<DBGroup> groups) {
        return new DBMembership(UUID.randomUUID(), users.get(user_id), groups.get(group_id));
    }
}