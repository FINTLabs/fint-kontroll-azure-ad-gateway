package no.fintlabs.azure;

import com.microsoft.graph.models.OnPremisesExtensionAttributes;
import com.microsoft.graph.models.User;
import no.fintlabs.ConfigUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.lenient;
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

    @Test
    public void makeSureUserAreHandledAsStudentWhenTheSameAttributeIsSetForBothStudentAndEmployee() {

        when(configUser.getUseSameIdNumAttribute()).thenReturn(true);
        when(configUser.getValidatorAttribute()).thenReturn("employeeType");
        when(configUser.getUserIdNumAttribute()).thenReturn("employeeId");
        lenient().when(configUser.getEmployeeValidator()).thenReturn("ansatt");
        lenient().when(configUser.getStudentValidator()).thenReturn("elev");


        User user = new User();
        user.id = "123";
        user.mail = "testuser@mail.com";
        user.userPrincipalName = "testuser@mail.com";
        user.accountEnabled = true;
        user.employeeId = "123";
        user.employeeType = "elev";

        AzureUser convertedUser = new AzureUser(user, configUser);

        assert(convertedUser.getIdpUserObjectId().equals(user.id));
        assert(convertedUser.getUserPrincipalName().equals(user.userPrincipalName));
        assert(convertedUser.getAccountEnabled() == true);
        assert(convertedUser.getStudentId().equals(user.employeeId));
        assert(convertedUser.getEmployeeId() == null);

    }

    @Test
    public void makeSureUserAreHandledAsEmployeeWhenTheSameAttributeIsSetForBothStudentAndEmployee() {

        when(configUser.getUseSameIdNumAttribute()).thenReturn(true);
        when(configUser.getValidatorAttribute()).thenReturn("employeeType");
        when(configUser.getUserIdNumAttribute()).thenReturn("employeeId");
        lenient().when(configUser.getEmployeeValidator()).thenReturn("ansatt");
        lenient().when(configUser.getStudentValidator()).thenReturn("elev");


        User user = new User();
        user.id = "123";
        user.mail = "testuser@mail.com";
        user.userPrincipalName = "testuser@mail.com";
        user.accountEnabled = true;
        user.employeeId = "123";
        user.employeeType = "ansatt";

        AzureUser convertedUser = new AzureUser(user, configUser);

        assert(convertedUser.getIdpUserObjectId().equals(user.id));
        assert(convertedUser.getUserPrincipalName().equals(user.userPrincipalName));
        assert(convertedUser.getAccountEnabled() == true);
        assert(convertedUser.getStudentId() == null);
        assert(convertedUser.getEmployeeId().equals(user.employeeId));

    }

    @Test
    public void makeSureUserAreHandledAsEmployeeWhenTheSameAttributeIsNOTSet() {

        when(configUser.getUseSameIdNumAttribute()).thenReturn(false);
        when(configUser.getEmployeeidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute10");
        when(configUser.getStudentidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute9");

        User user = new User();
        user.id = "123";
        user.mail = "testuser@mail.com";
        user.userPrincipalName = "testuser@mail.com";
        user.accountEnabled = true;
        user.onPremisesExtensionAttributes = new OnPremisesExtensionAttributes();
        user.onPremisesExtensionAttributes.extensionAttribute10 = "123";


        AzureUser convertedUser = new AzureUser(user, configUser);

        assert(convertedUser.getIdpUserObjectId().equals(user.id));
        assert(convertedUser.getUserPrincipalName().equals(user.userPrincipalName));
        assert(convertedUser.getAccountEnabled() == true);
        assert(convertedUser.getStudentId() == null);
        assert(convertedUser.getEmployeeId().equals(user.onPremisesExtensionAttributes.extensionAttribute10));

    }

    @Test
    public void makeSureUserAreHandledAsStudentWhenTheSameAttributeIsNOTSet() {

        when(configUser.getUseSameIdNumAttribute()).thenReturn(false);
        when(configUser.getEmployeeidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute10");
        when(configUser.getStudentidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute9");

        User user = new User();
        user.id = "123";
        user.mail = "testuser@mail.com";
        user.userPrincipalName = "testuser@mail.com";
        user.accountEnabled = true;
        user.onPremisesExtensionAttributes = new OnPremisesExtensionAttributes();
        user.onPremisesExtensionAttributes.extensionAttribute9 = "123";


        AzureUser convertedUser = new AzureUser(user, configUser);

        assert(convertedUser.getIdpUserObjectId().equals(user.id));
        assert(convertedUser.getUserPrincipalName().equals(user.userPrincipalName));
        assert(convertedUser.getAccountEnabled() == true);
        assert(convertedUser.getStudentId().equals(user.onPremisesExtensionAttributes.extensionAttribute9));
        assert(convertedUser.getEmployeeId() == null);

    }

}