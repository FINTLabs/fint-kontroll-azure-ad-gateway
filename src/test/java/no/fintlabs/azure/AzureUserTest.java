package no.fintlabs.azure;

import com.microsoft.graph.models.User;
import no.fintlabs.ConfigUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureUserTest {
    @Mock
    private ConfigUser configUser;

    @Test
    public void makeSureUserAttributeAccountEnabledIsAccountedFor() {


        when(configUser.getEmployeeidattribute()).thenReturn("Employeeid");
        when(configUser.getStudentidattribute()).thenReturn("Studentid");

        User user = new User();
        user.id = "123";
        user.mail = "testuser@mail.com";
        user.userPrincipalName = "testuser@mail.com";
        user.accountEnabled = true;

        AzureUser convertedUser = new AzureUser(user, configUser);

        assert(convertedUser.getId() == user.id );
        assert(convertedUser.getIdpUserObjectId() == user.id);
        assert(convertedUser.getUserPrincipalName() == user.userPrincipalName);
        assert(convertedUser.getAccountEnabled() == true);

    }

    @Test
    public void makeSureExternalUserAttributeAccountEnabledIsAccountedFor() {


        User user = new User();
        user.id = "123";
        user.mail = "testuser@mail.com";
        user.userPrincipalName = "testuser@mail.com";
        user.accountEnabled = true;

        AzureUserExternal convertedUser = new AzureUserExternal(user, configUser);

        assert(convertedUser.getIdpUserObjectId() == user.id);
        assert(convertedUser.getUserPrincipalName() == user.userPrincipalName);
        assert(convertedUser.getAccountEnabled() == true);

    }


    @Test
    public void makeSureUserConversionIsAsExpected() {

        /*when(configUser.getEmployeeidattribute()).thenReturn("country");
        when(configUser.getStudentidattribute()).thenReturn("preferredLanguage");

        User user = new User();
        user.id = "123";
        user.mail = "testuser@mail.com";
        user.userPrincipalName = "testuser@mail.com";

        AzureUser convertedUser = new AzureUser(user, configUser);

        assert(convertedUser.getId() == user.id );
        assert(convertedUser.getIdpUserObjectId() == user.id);
        assert(convertedUser.getMail() == user.mail );
        assert(convertedUser.getUserPrincipalName() == user.userPrincipalName);
        assert(convertedUser.getEmployeeId() == "SomeValue");
        assert(convertedUser.getStudentId() == "SomeValue3");*/

    }
}