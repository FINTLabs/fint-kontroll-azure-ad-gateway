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
    private String externaluserattribute;
    private String externaluservalue;
    private Integer userpagingsize;
    private Boolean enableExternalUsers;
    public List<String> AllAttributes(){
        List<String> AllAttribs = new ArrayList<>();
        AllAttribs.add(this.getStudentidattribute());
        AllAttribs.add(this.getEmployeeidattribute());
        AllAttribs.add(this.getMainorgunitidattribute());
        AllAttribs.add(this.getMainorgunitnameattribute());
        AllAttribs.add(this.getExternaluserattribute());
        AllAttribs.addAll(userAttributes);
        return AllAttribs;
    };

}