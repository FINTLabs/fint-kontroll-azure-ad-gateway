package no.fintlabs.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
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

    @Getter
    private String mainorgunitidattribute;
    @Getter
    private String mainorgunitnameattribute;
    @Getter
    private String employeeidattribute;
    @Getter
    private String studentidattribute;
    @Getter
    private String externaluserattribute;
    private String externaluservalue;
    @Getter
    private Integer userpagingsize;
    @Getter
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
    public String[] userAttributesDelta() {
        boolean wantsOnPremExtChild = false;


        List<String> raw = AllAttributes();

        List<String> cleaned = new ArrayList<>();
        for (String s : raw) {
            if (s == null) continue;
            s = s.trim();
            if (s.isEmpty()) continue;

            if (s.startsWith("onPremisesExtensionAttributes.")) {
                wantsOnPremExtChild = true;
                continue;
            }

            if (s.contains(".")) {
                continue;
            }

            cleaned.add(s);
        }

        cleaned.addAll(userAttributes);
        cleaned.add("userType");

        if (wantsOnPremExtChild && !cleaned.contains("onPremisesExtensionAttributes")) {
            cleaned.add("onPremisesExtensionAttributes");
        }

        LinkedHashSet<String> orderedUnique = new LinkedHashSet<>(cleaned);
        return orderedUnique.toArray(new String[0]);
    }

}