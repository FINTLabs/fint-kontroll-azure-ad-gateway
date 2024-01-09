package no.fintlabs.azure;

import com.microsoft.graph.models.*;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.ConfigUser;

import java.lang.reflect.Field;

@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor

@Log4j2
public class AzureUser {
        private String mail;
        private String id;
        private String userPrincipalName;
        private String employeeId;
        private String studentId;
        private String idpUserObjectId;
        private Boolean accountEnabled;

        public AzureUser(User user, ConfigUser configUser) {
                this.mail = user.mail;
                this.id = user.id;
                this.accountEnabled = user.accountEnabled;
                this.userPrincipalName = user.userPrincipalName;
                this.employeeId = getAttributeValue(user, configUser.getEmployeeidattribute());
                this.studentId = getAttributeValue(user, configUser.getStudentidattribute());
                this.idpUserObjectId = user.id;
        }

        public static String getAttributeValue(User user, String attributeName) {
                // Split the attribute name by dot to get the nested field names
                String[] attributeParts = attributeName.split("\\.");

                if (attributeParts[0].equals("onPremisesExtensionAttributes")) {
                        // Set attribute values
                        OnPremisesExtensionAttributes attributeValues = user.onPremisesExtensionAttributes;
                        try {
                                Field field = OnPremisesExtensionAttributes.class.getDeclaredField(attributeParts[1]);
                                field.setAccessible(true);
                                Object value = field.get(attributeValues);
                                if(value != null)
                                        return value.toString();
                        }
                        catch (Exception e)
                        {}
                }
                else
                {
                        try {
                                Field field = User.class.getDeclaredField(attributeName);
                                field.setAccessible(true);
                                Object value = field.get(user);
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




