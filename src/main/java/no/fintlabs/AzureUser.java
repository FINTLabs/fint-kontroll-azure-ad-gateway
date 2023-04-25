package no.fintlabs;

import com.microsoft.graph.models.User;
import lombok.*;

import java.util.ArrayList;
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
        private static final List<String> requiredAttributes = List.of (
                "id",
                "mail",
                "onPremisesExtensionAttributes",
                "userPrincipalName"
        );
        public static List<String> GetAttributes(ConfigUser configUser){
                // TODO: 25.04.2023 Concatinate optional attribs with required attribs
//                List<List<String>> Attribs = new ArrayList<>();
//                Attribs.add(requiredAttributes);
//                Attribs.add(configUser.getOptionaluserattributes());
                return requiredAttributes;
        };

        public AzureUser(User user, ConfigUser configUser) {
                this.id = user.id;
                this.mail = user.mail;
                this.userPrincipalName = user.userPrincipalName;
                this.employeeId = user.getClass().getName(configUser.getEmployeeidattribute());
                this.studentId = configUser.getStudentidattribute();

                //TODO: Parametrize attributes so they are configuratble
                //TODO: Make sure parameters are defined
                if (user.onPremisesExtensionAttributes != null) {
                        this.employeeId = user.onPremisesExtensionAttributes.extensionAttribute10;
                        this.studentId = user.onPremisesExtensionAttributes.extensionAttribute9;
                }
        }
}




