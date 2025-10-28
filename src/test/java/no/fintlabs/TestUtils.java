package no.fintlabs;

import com.microsoft.graph.models.Group;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.db.*;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TestUtils {

    public static class TestGroupData {
        public final List<Group> groups;
        public final List<String> removedMemberships;
        public final List<String> createdMemberships;

        public TestGroupData(List<Group> groups, List<String> removed, List<String> created) {
            this.groups = groups;
            this.removedMemberships = removed;
            this.createdMemberships = created;
        }
    }

    public static class DBObjectListOrchestratorTest extends DBObjectListOrchestrator {
        private Random rand = new Random();
        public DBObjectListOrchestratorTest() {
        }
        public void generateNRandomUsers(int nUsers) {
            for (int i = 0; i < nUsers; i++) {
                this.getUsers().put(UUID.randomUUID().toString(), new DBUser(UUID.randomUUID()));
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
            List<String> userList = getUsers().getHashMap().keySet().stream().toList();
            for (int i=0; i<nGroups; i++) {
                DBGroup newGroup = new DBGroup(UUID.randomUUID());
                String newString = UUID.randomUUID().toString();
                getGroups().put(newString, newGroup);
                for (int j=0; j < rand.nextInt((highMemberNumber - lowMemberNumber) + 1) + lowMemberNumber; j++) {
                    getMemberships().put(
                            UUID.randomUUID().toString(),
                            new DBMembership(
                                    UUID.randomUUID(),
                                    getUsers().get(
                                            userList.get(
                                                    rand.nextInt(userList.size())
                                            )
                                    ),
                                    newGroup) );
                }
            }
        }
    }
}
