package no.fintlabs;

import com.microsoft.graph.models.Group;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azure.HashKey;
import no.fintlabs.core.*;
import no.fintlabs.core.entity.CoreGroup;
import no.fintlabs.core.entity.CoreMembership;
import no.fintlabs.core.entity.CoreUser;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j

public class TestUtils {

    public static CoreUser getRandomUser() {
        return new CoreUser(HashKey.createRandomHashKey());
    }

    public static class TestGroupData {
        public final List<Group> groups;
        public final List<Tuple2<HashKey,Tuple2<UUID, Long>>> removedMemberships;
        public final List<Tuple2<HashKey,Tuple2<UUID, Long>>> createdMemberships;
        public final List<UUID> removedUsers;
        public final List<UUID> addedUsers;
        public TestGroupData(List<Group> groups, List<Tuple2<HashKey,Tuple2<UUID, Long>>> createdMemberships, List<Tuple2<HashKey,Tuple2<UUID, Long>>> removedMemberships, List<UUID> addedUsers, List<UUID> removedUsers) {
            this.groups = groups;
            this.createdMemberships = createdMemberships;
            this.removedMemberships = removedMemberships;
            this.removedUsers = removedUsers;
            this.addedUsers = addedUsers;
        }
    }

    public static class CoreObjectListOrchestratorTest extends CoreObjectListOrchestrator {
        private final Random rand = new Random();
        public CoreObjectListOrchestratorTest() {
        }
        public void generateNRandomUsers(int nUsers) {
            for (int i = 0; i < nUsers; i++) {
                this.getUsers().put(UUID.randomUUID(), new CoreUser(HashKey.createHashKey(UUID.randomUUID().toString())));
            }
        }
        public void generateNRandomGroupsWithNMemberships(int nGroups, int lowMemberNumber, int highMemberNumber) {
            if (lowMemberNumber > highMemberNumber) {
                // log.error("lowMemberNumber > highMemberNumber");
                return;
            }
            if (highMemberNumber > getUsers().getHashMap().size()) {
                // log.error("Asked for members with more than " + highMemberNumber + " members");
                return;
            }
            List<UUID> userList = getUsers().getHashMap().keySet().stream().toList();
            for (int i=0; i<nGroups; i++) {
                CoreGroup newGroup = new CoreGroup(HashKey.createHashKey(UUID.randomUUID().toString()), "testgroup-" + i);
                Long newGroupID = rand.nextLong();
                getGroups().put(newGroupID, newGroup);
                for (int j=0; j < rand.nextInt((highMemberNumber - lowMemberNumber) + 1) + lowMemberNumber; j++) {
                    // Pick random user
                    UUID userId = userList.get(rand.nextInt(userList.size()));
                    getMemberships().put(
                            CoreMembershipMapper.toCoreMembershipHashKey(userId, newGroupID),
                            new CoreMembership(
                                    HashKey.createHashKey(UUID.randomUUID()),
                                    getUsers().get(userId),
                                    newGroup) );
                }
            }
        }
    }
}
