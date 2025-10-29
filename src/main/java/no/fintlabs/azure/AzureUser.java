package no.fintlabs.azure;

import com.microsoft.graph.models.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.config.ConfigUser;
import java.util.Objects;

@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor

@Slf4j
@EqualsAndHashCode
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
}




