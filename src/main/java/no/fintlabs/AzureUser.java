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
        private String employeeId;
        private String studentId;

        public AzureUser(User user, ConfigUser configUser) {

                this.id = user.id;
                this.mail = user.mail;
                this.userPrincipalName = user.userPrincipalName;
                this.employeeId = getAttributeValue(user, configUser.getEmployeeidattribute());
                this.studentId = getAttributeValue(user, configUser.getStudentidattribute());
        }

        private String getAttributeValue(User user, String attributeName) {
                // Split the attribute name by dot to get the nested field names
                String[] attributeParts = attributeName.split("\\.");

                if (attributeParts[0].equals("onPremisesExtensionAttributes")) {
                        // Set attribute values
                        OnPremisesExtensionAttributes attributeValues = user.onPremisesExtensionAttributes;
                        try {
                                Field _field = OnPremisesExtensionAttributes.class.getDeclaredField(attributeParts[1]);
                                _field.setAccessible(true);
                                Object value = _field.get(attributeValues);
                                if(value != null)
                                        return value.toString();
                        }
                        catch (Exception e)
                        {}
                }
                else
                {
                        try {
                                Field _field = User.class.getDeclaredField(attributeName);
                                _field.setAccessible(true);
                                Object value = _field.get(user);
                                if(value != null)
                                        return value.toString();
                        }
                        catch (Exception e)
                        {}
                }
                // Return null if no attribute values
                return null;
        }


}




