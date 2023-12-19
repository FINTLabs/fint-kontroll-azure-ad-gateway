package no.fintlabs.azure;

import no.fintlabs.ConfigUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AzureUserTest {
    @Mock
    private ConfigUser configUser;

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