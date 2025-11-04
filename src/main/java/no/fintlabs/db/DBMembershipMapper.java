package no.fintlabs.db;

import no.fintlabs.azure.AzureGroupMembership;
import no.fintlabs.db.entity.DBGroup;
import no.fintlabs.db.entity.DBMembership;
import no.fintlabs.db.entity.DBUser;

import java.util.UUID;

public class DBMembershipMapper {
    static public DBMembership toDBMembership(
            AzureGroupMembership membership,
            DBObjectList<String, DBUser> users,
            DBObjectList<String, DBGroup> groups) {
        return new DBMembership(
                users.get(membership.getUser_id()),
                groups.get(membership.getGroup_id())
        );
    }
    static public DBMembership toDBMembership(
            String user_id,
            String group_id,
            DBObjectList<String, DBUser> users,
            DBObjectList<String, DBGroup> groups) {
        return new DBMembership(
                users.get(user_id),
                groups.get(group_id)
        );
    }
}