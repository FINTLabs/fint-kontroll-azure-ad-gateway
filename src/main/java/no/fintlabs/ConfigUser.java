package no.fintlabs;

import com.azure.core.annotation.Get;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//@Service
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigUser {
    private static List<String> requiredAttributes = Arrays.asList (
            "id",
            "mail",
            "onPremisesExtensionAttributes",
            "userPrincipalName"
    );

    private List<String> optionaluserattributes = Collections.emptyList();
    private String employeeidattribute;
    private String studentidattribute;
    public List<String> AllAttributes(){
        List<String> AllAttribs = new ArrayList<>();
        AllAttribs.add(this.getStudentidattribute());
        AllAttribs.add(this.getEmployeeidattribute());
        AllAttribs.addAll(requiredAttributes);
        if(optionaluserattributes.isEmpty() == false) {
            AllAttribs.addAll(optionaluserattributes);
        };
        return AllAttribs;
    };

}