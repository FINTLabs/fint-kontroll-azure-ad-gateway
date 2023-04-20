package no.fintlabs;

import com.microsoft.graph.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor

//@Configuration
//@ConfigurationProperties(prefix = "fint.flyt.azure-ad-gateway")
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

/*        @Value("${optionaluserattributes}")
        private final List<String> optionaluserattributes;*/

        /*public void GetOptionalUserAttributes() {
                List<String> allUserAttributes = optionaluserattributes.getAllUserAttributes();

        }*/
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




