package no.fintlabs.azure;

import com.microsoft.graph.models.*;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.ConfigUser;

import java.lang.reflect.Field;
import java.util.Objects;

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
                this.mail = user.getMail();
                this.id = user.getId();
                this.accountEnabled = user.getAccountEnabled();
                this.userPrincipalName = user.getUserPrincipalName();
                this.employeeId = getAttributeValue(user, configUser.getEmployeeidattribute());
                this.studentId = getAttributeValue(user, configUser.getStudentidattribute());
                this.idpUserObjectId = user.getId();
        }

        public static String getAttributeValue(User user, String attributeName) {
                // Split the attribute name by dot to get the nested field names
                if(attributeName == null) {
                        return null;
                }

                String[] attributeParts = attributeName.split("\\.");

                if (attributeParts[0].equals("onPremisesExtensionAttributes")) {
                        // Set attribute values
                        OnPremisesExtensionAttributes attributeValues = user.getOnPremisesExtensionAttributes();
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

        @Override
        public boolean equals(Object o) {
                if (this == o) return true; // If the references are the same
                if (o == null || getClass() != o.getClass()) return false; // Check type compatibility
                AzureUser azureUser = (AzureUser) o; // Cast and compare
                return Objects.equals(mail, azureUser.mail) &&
                        Objects.equals(id, azureUser.id) &&
                        Objects.equals(userPrincipalName, azureUser.userPrincipalName) &&
                        Objects.equals(employeeId, azureUser.employeeId) &&
                        Objects.equals(studentId, azureUser.studentId) &&
                        Objects.equals(idpUserObjectId, azureUser.idpUserObjectId) &&
                        Objects.equals(accountEnabled, azureUser.accountEnabled);
        }

        @Override
        public int hashCode() {
                return Objects.hash(mail, id, userPrincipalName, employeeId, studentId, idpUserObjectId, accountEnabled);
        }
}




