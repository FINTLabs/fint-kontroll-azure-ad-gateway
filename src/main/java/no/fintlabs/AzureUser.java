package no.fintlabs;

import com.microsoft.graph.models.*;
import lombok.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@RequiredArgsConstructor
public class AzureUser extends BaseObject {
        private User userObject;

        public AzureUser(User user) {
                this.id = user.id;
                this.userObject =  user;
                Map<String, Object> results = new HashMap<>();
                //Class<?> User = com.microsoft.graph.models.User.class;
                Field[] fields = com.microsoft.graph.models.User.class.getDeclaredFields();
                for (Field field : fields) {
                        // make the field accessible to be able to read its value
                        field.setAccessible(true);

                        try {
                                // get the value of the field from the group object
                                Object value = field.get(user);

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




