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
        //private User userObject;
        protected String id;
        protected String mail;
        protected String onPremisesExtensionAttributes;
        protected String userPrincipalName;
        public AzureUser(User user) {
                //this.id = user.id;
                //this.userObject =  user;
                //Map<String, Object> results = new HashMap<>();
                //Field[] fields = com.microsoft.graph.models.User.class.getDeclaredFields();
                Field[] azfields =  AzureUser.class.getDeclaredFields();
                for (Field field : azfields) {
                        // make the field accessible to be able to read its value
                        field.setAccessible(true);
                        try {
                                // get the value of the field from the user object
                                Object value = field.get(user);
                                // add the value to the results map if it's not null
                                if (value != null) {
                                        field.set(this, value);
                                        //results.put(field.getName(), value);
                                }
                        } catch (IllegalAccessException e) {
                                // handle the exception if the field is not accessible
                                e.printStackTrace();
                        }
                }
        }
}




