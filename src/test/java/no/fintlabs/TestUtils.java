package no.fintlabs;

import com.microsoft.graph.models.Group;

import java.util.List;

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
}
