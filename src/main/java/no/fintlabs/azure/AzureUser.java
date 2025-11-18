package no.fintlabs.azure;

import com.microsoft.graph.models.*;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.config.ConfigUser;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor

@Slf4j
@EqualsAndHashCode
public class AzureUser implements Serializable {
        @Serial
        private String mail;
        @Serial
        private String id;
        @Serial
        private String userPrincipalName;
        @Serial
        private String employeeId;
        @Serial
        private String studentId;
        @Serial
        private String idpUserObjectId;
        @Serial
        private Boolean accountEnabled;
        @Serial
        private String validatorAttribute;

        public AzureUser(User user, ConfigUser configUser) {
            this.mail = user.getMail();
            this.id = user.getId();
            this.accountEnabled = user.getAccountEnabled();
            this.userPrincipalName = user.getUserPrincipalName();
            this.idpUserObjectId = user.getId();
            if (!configUser.getUseSameIdNumAttribute()) {
                this.employeeId = getAttributeValue(user, configUser.getEmployeeidattribute());
                this.studentId = getAttributeValue(user, configUser.getStudentidattribute());
                return;
            }

            String valAttrValue = getAttributeValue(user, configUser.getValidatorAttribute());
            if (valAttrValue == null) return;

            String userIdNumAttr = configUser.getUserIdNumAttribute();
            String userIdNumValue  = getAttributeValue(user, userIdNumAttr);

            if (valAttrValue.contains(configUser.getEmployeeValidator())) {
                this.employeeId = userIdNumValue;
            } else if (valAttrValue.contains(configUser.getStudentValidator())) {
                this.studentId = userIdNumValue;
            }

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




