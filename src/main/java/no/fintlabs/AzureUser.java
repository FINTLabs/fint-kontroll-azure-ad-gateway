package no.fintlabs;

import com.microsoft.graph.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor


public class AzureUser extends BaseObject {

        private String userPrincipalName;
        private String mail;
        private String employeeId;
        private String studentId;
        public static final String[] requiredAttributes = {
                "id",
                "userPrincipalName",
                "mail",
                "onPremisesExtensionAttributes",

        };
        @Autowired
        private OptionalUserAttributes optionaluserattributes;

        public void GetOptionalUserAttributes() {
                List<String> allUserAttributes = optionaluserattributes.getAllUserAttributes();

        }
        public AzureUser(User user) {
                GetOptionalUserAttributes();
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




