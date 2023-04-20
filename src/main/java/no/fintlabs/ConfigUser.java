package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

//@Service
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigUser {

 //   @Value("fint.flyt.azure-ad-gateway")
    private List<String> optionaluserattributes = Collections.emptyList();

    /*public List<String> getAllUserAttributes() {
        List<String> allUserAttributes = new ArrayList<>();
        allUserAttributes.addAll(Arrays.asList(AzureUser.requiredAttributes));

        if (optionalUserAttributes != null && !optionalUserAttributes.isEmpty()) {
            allUserAttributes.addAll(optionalUserAttributes);
        }

        return allUserAttributes;
    }*/
}