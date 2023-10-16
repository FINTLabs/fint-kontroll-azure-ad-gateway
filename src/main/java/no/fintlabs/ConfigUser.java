package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//@Service
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigUser {

    private static List<String> userAttributes = Arrays.asList (
            "id",
            "mail",
            "onPremisesExtensionAttributes",
            "userPrincipalName",
            "displayname",
            "givenname",
            "surname",
            "onPremisesUserPrincipalName",
            "onPremisesSamAccountName"
    );

    private String mainorgunitidattribute;
    private String mainorgunitnameattribute;
    private String employeeidattribute;
    private String studentidattribute;
    public List<String> AllAttributes(){
        List<String> AllAttribs = new ArrayList<>();
        AllAttribs.add(this.getStudentidattribute());
        AllAttribs.add(this.getEmployeeidattribute());
        AllAttribs.add(this.getMainorgunitidattribute());
        AllAttribs.add(this.getMainorgunitnameattribute());
        AllAttribs.addAll(userAttributes);
        return AllAttribs;
    };

}