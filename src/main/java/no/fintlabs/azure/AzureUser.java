package no.fintlabs.azure;

import com.microsoft.graph.models.*;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.ConfigUser;
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
                if (attributeName == null) {
                        return null;
                }

                String[] attributeParts = attributeName.split("\\.");
                if (attributeParts[0].equals("onPremisesExtensionAttributes")) {
                        OnPremisesExtensionAttributes attributeValues = user.getOnPremisesExtensionAttributes();
                        try {
                                String strVal = attributeValues.getBackingStore().get(attributeParts[1]);
                                if (strVal != null)
                                        return strVal;
                        } catch (NullPointerException e) {
                                log.debug("getAttributeValue expected {}, but this is not found: {}", attributeName, e.getMessage());
                        }
                } else {
                        String strVal = user.getBackingStore().get(attributeName);
                        if (strVal != null) {
                                return strVal;
                        }
                }
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




