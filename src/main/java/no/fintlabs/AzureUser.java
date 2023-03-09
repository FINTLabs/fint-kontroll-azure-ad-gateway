package no.fintlabs;

import com.microsoft.graph.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Setter
@Getter
@AllArgsConstructor
public class AzureUser {
        private String userPrincipalName;
        private String id;
        private String mail;
        private String employeeId;
        private String studentId;

        public AzureUser(User user) {
                this.id = user.id;
                this.mail = user.mail;
                this.userPrincipalName = user.userPrincipalName;
                //TODO: Parametrize attributes so they are configuratble
                //TODO: Make sure parameters are defined
                if (user.onPremisesExtensionAttributes != null) {
                        this.employeeId = user.onPremisesExtensionAttributes.extensionAttribute10;
                        this.studentId = user.onPremisesExtensionAttributes.extensionAttribute9;
                }
        }
}