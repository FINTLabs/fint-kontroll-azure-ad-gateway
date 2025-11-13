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
            "accountEnabled",
            "mail",
            "mobilePhone",
            "onPremisesExtensionAttributes",
            "userPrincipalName",
            "displayName",
            "givenName",
            "surname",
            "onPremisesUserPrincipalName",
            "onPremisesSamAccountName"
    );

    private String mainorgunitidattribute;
    private String mainorgunitnameattribute;
    private String employeeidattribute;
    private String studentidattribute;
    private String externaluserattribute;
    private String externaluservalue;
    private Boolean enableExternalUsers;
    private Boolean useSameIdNumAttribute;
    private String userIdNumAttribute;
    private String studentValidator;
    private String employeeValidator;
    private String validatorAttribute;

    public List<String> AllAttributes(){
        List<String> AllAttribs = new ArrayList<>();
        if(!useSameIdNumAttribute)
        {
            AllAttribs.add(this.getStudentidattribute());
            AllAttribs.add(this.getEmployeeidattribute());
        }
        else {
            AllAttribs.add(this.getUserIdNumAttribute());
            AllAttribs.add(this.getValidatorAttribute());
        }
        AllAttribs.add(this.getMainorgunitidattribute());
        AllAttribs.add(this.getMainorgunitnameattribute());
        AllAttribs.add(this.getExternaluserattribute());
        AllAttribs.addAll(userAttributes);
        return AllAttribs;
    };

}