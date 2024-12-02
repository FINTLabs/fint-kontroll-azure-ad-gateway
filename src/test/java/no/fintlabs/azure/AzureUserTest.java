package no.fintlabs.azure;

import com.microsoft.graph.models.User;
import no.fintlabs.ConfigUser;
import no.fintlabs.cache.FintCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureUserTest {
    @Mock
    private ConfigUser configUser;

    @Mock
    private FintCache<String, AzureUser> entraIdUserCache;

    @Mock
    private AzureUserProducerService azureUserProducerService;

    @Test
    public void makeSureUserAttributeAccountEnabledIsAccountedFor() {


        when(configUser.getEmployeeidattribute()).thenReturn("Employeeid");
        when(configUser.getStudentidattribute()).thenReturn("Studentid");

        User user = new User();
        user.setId("123");
        user.setMail("testuser@mail.com");
        user.setUserPrincipalName("testuser@mail.com");
        user.setAccountEnabled(true);

        AzureUser convertedUser = new AzureUser(user, configUser);

        assert(convertedUser.getId() == user.getId() );
        assert(convertedUser.getIdpUserObjectId() == user.getId());
        assert(convertedUser.getUserPrincipalName() == user.getUserPrincipalName());
        assert(convertedUser.getAccountEnabled() == true);

    }

    @Test
    public void makeSureExternalUserAttributeAccountEnabledIsAccountedFor() {


        User user = new User();
        user.setId("123");
        user.setMail("testuser@mail.com");
        user.setUserPrincipalName("testuser@mail.com");
        user.setAccountEnabled(true);

        AzureUserExternal convertedUser = new AzureUserExternal(user, configUser);

        assert(convertedUser.getIdpUserObjectId() == user.getId());
        assert(convertedUser.getUserPrincipalName() == user.getUserPrincipalName());
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