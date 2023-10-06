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

public class AzureUserExternal extends AzureUser {
    private String firstName;
    private String lastName;
    private String mobilePhone;
    private String email;
    private String mainOrganisationUnitName;
    private String mainOrganisationUnitId;
    private String userName;
    public AzureUserExternal(User user, ConfigUser configUser) {
        super(user, configUser);
        this.firstName = user.givenName;
        this.lastName = user.surname;
        this.mobilePhone = user.mobilePhone;
        this.email = user.mail;
        this.mainOrganisationUnitName = getAttributeValue(user, configUser.getMainorgunitnameattribute());
        this.mainOrganisationUnitId = getAttributeValue(user, configUser.getMainorgunitidattribute());
    }
}
