package no.fintlabs;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.DirectoryObjectCollectionPage;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class AzureGroup extends BaseObject {
    private DirectoryObjectCollectionPage members;
    private String name;
    public AzureGroup(Group group) {
        this.id = group.id;
        this.name = group.displayName;
        this.members = group.members;

        Map<String, Object> results = new HashMap<>();
        Class<?> Group = com.microsoft.graph.models.Group.class;
        Field[] fields = Group.getDeclaredFields();
        for (Field field : fields) {
            // make the field accessible to be able to read its value
            field.setAccessible(true);

            try {
                // get the value of the field from the group object
                Object value = field.get(group);

                // add the value to the results map if it's not null
                if (value != null) {
                    results.put(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                // handle the exception if the field is not accessible
                e.printStackTrace();
            }
        }
    }
}