package no.fintlabs.azure;

import com.microsoft.graph.models.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.ConfigUser;

import java.util.Objects;

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
        this.idpUserObjectId = user.id;
        this.userPrincipalName = user.userPrincipalName;
        this.accountEnabled = user.accountEnabled;
        this.firstName = user.givenName;
        this.lastName = user.surname;
        this.mobilePhone = user.mobilePhone;
        this.email = user.mail;
        if (!user.additionalDataManager().isEmpty() && user.additionalDataManager().containsKey(configUser.getMainorgunitnameattribute())) {
            this.mainOrganisationUnitName = user.additionalDataManager().get(configUser.getMainorgunitnameattribute()).getAsString();
        }
        if (!user.additionalDataManager().isEmpty() && user.additionalDataManager().containsKey(configUser.getMainorgunitidattribute())) {

            this.mainOrganisationUnitId = user.additionalDataManager().get(configUser.getMainorgunitidattribute()).getAsString();
        }
        this.userName = user.mail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // If the references are the same
        if (o == null || getClass() != o.getClass()) return false; // Check type compatibility
        AzureUserExternal azureUserExternal = (AzureUserExternal) o; // Cast and compare
        return Objects.equals(email, azureUserExternal.email) &&
                Objects.equals(idpUserObjectId, azureUserExternal.idpUserObjectId) &&
                Objects.equals(userPrincipalName, azureUserExternal.userPrincipalName) &&
                Objects.equals(accountEnabled, azureUserExternal.accountEnabled) &&
                Objects.equals(mainOrganisationUnitName, azureUserExternal.mainOrganisationUnitName) &&
                Objects.equals(mainOrganisationUnitId, azureUserExternal.mainOrganisationUnitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, idpUserObjectId, userPrincipalName, accountEnabled, mainOrganisationUnitName, mainOrganisationUnitId);
    }
}
