package no.fintlabs;

import com.microsoft.graph.models.Group;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azure.HashKey;
import no.fintlabs.db.*;
import no.fintlabs.db.entity.DBGroup;
import no.fintlabs.db.entity.DBMembership;
import no.fintlabs.db.entity.DBUser;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j

public class TestUtils {

    @AllArgsConstructor
    public static class TestGroupData {
        public final List<Group> groups;
        public final List<UUID> removedMemberships;
        public final List<UUID> createdMemberships;
        public TestGroupData(List<Group> groups, List<UUID> removedMemberships, List<UUID> createdMemberships) {
            this.groups = groups;
            this.removedMemberships = removedMemberships;
            this.createdMemberships = createdMemberships;
        }
    }

    public static class DBObjectListOrchestratorTest extends DBObjectListOrchestrator {
        private Random rand = new Random();
        public DBObjectListOrchestratorTest() {
        }
        public void generateNRandomUsers(int nUsers) {
            for (int i = 0; i < nUsers; i++) {
                this.getUsers().put(UUID.randomUUID(), new DBUser(HashKey.createHashKey(UUID.randomUUID().toString())));
            }
        }
        public void generateNRandomGroupsWithNMemberships(int nGroups, int lowMemberNumber, int highMemberNumber) {
            if (lowMemberNumber > highMemberNumber) {
                // log.error("lowMemberNumber > highMemberNumber");
                return;
            };
            if (highMemberNumber > getUsers().getHashMap().size()) {
                // log.error("Asked for members with more than " + highMemberNumber + " members");
                return;
            }
            List<UUID> userList = getUsers().getHashMap().keySet().stream().toList();
            for (int i=0; i<nGroups; i++) {
                DBGroup newGroup = new DBGroup(HashKey.createHashKey(UUID.randomUUID().toString()));
                UUID newGroupID = UUID.randomUUID();
                getGroups().put(newGroupID, newGroup);
                for (int j=0; j < rand.nextInt((highMemberNumber - lowMemberNumber) + 1) + lowMemberNumber; j++) {
                    // Pick random user
                    UUID userId = userList.get(rand.nextInt(userList.size()));
                    getMemberships().put(
                            DBMembershipMapper.toDBMembershipHashKey(userId, newGroupID),
                            new DBMembership(
                                    HashKey.createHashKey(UUID.randomUUID()),
                                    getUsers().get(userId),
                                    newGroup) );
                }
            }
        }
    }
}
