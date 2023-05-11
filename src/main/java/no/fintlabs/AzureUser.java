package no.fintlabs;

import com.microsoft.graph.models.*;
import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;

@Setter
@Getter
@RequiredArgsConstructor
@Log4j2
public class AzureUser {


        private String id;
        private String mail;
        private String userPrincipalName;
        private String displayname;
        private String givenname;
        private String surname;
        private String onPremisesUserPrincipalName;
        private String employeeId;
        private String studentId;

        public AzureUser(User user, ConfigUser configUser) {

                this.id = user.id;
                this.mail = user.mail;
                this.displayname = user.displayName;
                this.surname = user.surname;
                this.givenname = user.givenName;
                this.userPrincipalName = user.userPrincipalName;
                this.onPremisesUserPrincipalName = user.onPremisesUserPrincipalName;
                this.employeeId = getAttributeValue(user, configUser.getEmployeeidattribute());
                this.studentId = getAttributeValue(user, configUser.getStudentidattribute());
        }


        public static String getAttributeValue(User user, String attributeName) {
                // Split the attribute name by dot to get the nested field names
                String[] attributeParts = attributeName.split("\\.");

                if (attributeParts[0].equals("onPremisesExtensionAttributes")) {
                        OnPremisesExtensionAttributes attributeValues = user.onPremisesExtensionAttributes;
                        try {
                                Field _field = OnPremisesExtensionAttributes.class.getDeclaredField(attributeParts[1]);
                                _field.setAccessible(true);
                                Object value = _field.get(attributeValues);
                                if(_field.getName().endsWith(attributeParts[1]) && value != null)
                                        return value.toString();
                        }
                        catch (Exception e)
                        {}
//                        for (Field field : fields) {
//                                // make the field accessible to be able to read its value
//                                field.setAccessible(true);
//                                try {
//                                        // get the value of the field from the group object
//                                        Object value = field.get(attributeValues);
//
//                                        // add the value to the results map if it's not null
//                                        if(field.getName().endsWith(attributeParts[1]))
//                                                if (value != null)
//                                                        return value.toString();
//
//                                } catch (IllegalAccessException e) {
//                                        // handle the exception if the field is not accessible
//                                        e.printStackTrace();
//                        }
//                        }
                }
                else
                {
                        try {
                                Field _field = OnPremisesExtensionAttributes.class.getDeclaredField(attributeName);
                                _field.setAccessible(true);
                                Object value = _field.get(user);
                                if(_field.getName().endsWith(attributeName) && value != null)
                                        return value.toString();
                        }
                        catch (Exception e)
                        {}
//                        Field[] fields = User.class.getDeclaredFields();
//                        for (Field field : fields) {
//                                // make the field accessible to be able to read its value
//                                field.setAccessible(true);
//                                try {
//                                        // get the value of the field from the group object
//                                        Object value = field.get(user);
//
//                                        // add the value to the results map if it's not null
//                                        if(field.getName().endsWith(attributeName))
//                                                if (value != null) {
//                                                        return value.toString();
//                                                }
//                                } catch (IllegalAccessException e) {
//                                        // handle the exception if the field is not accessible
//                                        e.printStackTrace();
//                                }
//                        }
                }

                // Return the attribute value
                return null;
        }


}




