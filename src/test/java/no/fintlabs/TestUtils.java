package no.fintlabs;

import com.microsoft.graph.models.Group;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azure.HashKey;
import no.fintlabs.core.*;
import no.fintlabs.core.entity.CoreDevice;
import no.fintlabs.core.entity.CoreGroup;
import no.fintlabs.core.entity.CoreMembership;
import no.fintlabs.core.entity.CoreUser;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j

public class TestUtils {

    public static class Outputter {
        public void write(String out) {
            System.out.println(out);
        }
        public void writeError(String out) {
            System.out.println(out);
        }
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static CoreUser getRandomUser() {
        return new CoreUser(HashKey.createRandomHashKey());
    }

    public static void addNRandomUsersToList(CoreObjectListReactive<UUID, CoreUser> userList, int nUsers) {
        for (int i = 0; i < nUsers; i++) {
            userList.put(UUID.randomUUID(), getRandomUser());
        }
    }

    public static CoreDevice getRandomDevice() {
        return new CoreDevice(HashKey.createRandomHashKey());
    }

    public static void addNRandomDevicesToList(CoreObjectListReactive<UUID, CoreDevice> deviceList, int nDevices) {
        for (int i = 0; i < nDevices; i++) {
            deviceList.put(UUID.randomUUID(), getRandomDevice());
        }
    }

    public static Map<UUID, CoreUser> pickNRandomUsers(CoreObjectList<UUID, CoreUser> userList, int nUsers) {

        if (nUsers >= userList.getHashMap().size()) {
            throw new IllegalArgumentException("nUsers must be less than userList.getHashMap().size. " + nUsers + " / " + userList.getHashMap().size());
        }
        // Convert entries to a list for random access
        List<Map.Entry<UUID, CoreUser>> entries = new ArrayList<>(userList.getHashMap().entrySet());

        if (entries.isEmpty()) {
            return Collections.emptyMap();
        }

        // Shuffle the list
        Collections.shuffle(entries);

        // Limit to n or size of list and collect back to a Map
        return entries.stream()
                .limit(nUsers)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<UUID, CoreDevice> pickNRandomDevices(CoreObjectList<UUID, CoreDevice> deviceList, int nDevices) {

        if (nDevices >= deviceList.getHashMap().size()) {
            throw new IllegalArgumentException("nUsers must be less than userList.getHashMap().size. "
                    + nDevices + " / " + deviceList.getHashMap().size());
        }
        // Convert entries to a list for random access
        List<Map.Entry<UUID, CoreDevice>> entries = new ArrayList<>(deviceList.getHashMap().entrySet());

        if (entries.isEmpty()) {
            return Collections.emptyMap();
        }

        // Shuffle the list
        Collections.shuffle(entries);

        // Limit to n or size of list and collect back to a Map
        return entries.stream()
                .limit(nDevices)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }



    public static void removeNRandomUsersFromList(CoreObjectListReactive<UUID, CoreUser> userList, int nUsers) {
        Map<UUID, CoreUser> randomUsers = pickNRandomUsers(userList, nUsers);
        randomUsers.forEach((uuid, user) -> userList.remove(uuid));
    }

    public static void removeNRandomDevicesFromList(CoreObjectListReactive<UUID, CoreDevice> deviceList, int nDevices) {
        Map<UUID, CoreDevice> randomDevices = pickNRandomDevices(deviceList, nDevices);
        randomDevices.forEach((uuid, device) -> deviceList.remove(uuid));
    }

    public static void updateNRandomUsersInLIst(CoreObjectListReactive<UUID, CoreUser> userList, int nUsers) {
        Map<UUID, CoreUser> randomUsers = pickNRandomUsers(userList, nUsers);
        randomUsers.forEach((uuid, user) -> userList.put(uuid, getRandomUser()));
    }

    public static void updateNRandomDevicesInLIst(CoreObjectListReactive<UUID, CoreDevice> deviceList, int nDevices) {
        Map<UUID, CoreDevice> randomDevices = pickNRandomDevices(deviceList, nDevices);
        randomDevices.forEach((uuid, device) -> deviceList.put(uuid, getRandomDevice()));
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
