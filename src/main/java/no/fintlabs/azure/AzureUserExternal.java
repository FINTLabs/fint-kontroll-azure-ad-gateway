package no.fintlabs.azure;

import com.microsoft.graph.models.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.ConfigUser;

@Setter
@Getter
@RequiredArgsConstructor
@Log4j2

public class AzureUserExternal  {
    private String firstName;
    private String lastName;
    private String mobilePhone;
    private String email;
    private String mainOrganisationUnitName;
    private String mainOrganisationUnitId;
    private String userName;
    private String idpUserObjectId;
    private String userPrincipalName;
    private Boolean accountEnabled;

    public AzureUserExternal(User user, ConfigUser configUser) {
        this.idpUserObjectId = user.getId();
        this.userPrincipalName = user.getUserPrincipalName();
        this.accountEnabled = user.getAccountEnabled();
        this.firstName = user.getGivenName();
        this.lastName = user.getSurname();
        this.mobilePhone = user.getMobilePhone();
        this.email = user.getMail();
        if (!user.getAdditionalData().isEmpty() && user.getAdditionalData().containsKey(configUser.getMainorgunitnameattribute())) {
            this.mainOrganisationUnitName = user.getAdditionalData().get(configUser.getMainorgunitnameattribute()).toString();
        }
        if (!user.getAdditionalData().isEmpty() && user.getAdditionalData().containsKey(configUser.getMainorgunitidattribute())) {

            this.mainOrganisationUnitId = user.getAdditionalData().get(configUser.getMainorgunitidattribute()).toString();
        }
        this.userName = user.getMail();
    }
}
