package no.fintlabs.azure;

import com.microsoft.graph.models.OnPremisesExtensionAttributes;
import com.microsoft.graph.models.User;
import no.fintlabs.ConfigUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
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

        assertAll(
                () -> assertEquals(user.id, convertedUser.getIdpUserObjectId()),
                () -> assertEquals(user.userPrincipalName, convertedUser.getUserPrincipalName()),
                () -> assertEquals(convertedUser.getIdpUserObjectId(), user.id),
                () -> assertTrue(convertedUser.getAccountEnabled())
        );


    }

    @Test
    public void makeSureExternalUserAttributeAccountEnabledIsAccountedFor() {


        User user = new User();
        user.id = "123";
        user.mail = "testuser@mail.com";
        user.userPrincipalName = "testuser@mail.com";
        user.accountEnabled = true;

        AzureUserExternal convertedUser = new AzureUserExternal(user, configUser);

        assertAll(
                () -> assertEquals(user.id, convertedUser.getIdpUserObjectId()),
                () -> assertEquals(user.userPrincipalName, convertedUser.getUserPrincipalName()),
                () -> assertTrue(convertedUser.getAccountEnabled())
        );
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
        user.employeeType = "vgs_elev";

        AzureUser convertedUser = new AzureUser(user, configUser);

        assertAll(
                () -> assertEquals(user.id, convertedUser.getIdpUserObjectId()),
                () -> assertEquals(user.userPrincipalName, convertedUser.getUserPrincipalName()),
                () -> assertTrue(convertedUser.getAccountEnabled()),
                () -> assertTrue(convertedUser.getStudentId().contains(user.employeeId)),
                () -> assertNull(convertedUser.getEmployeeId())
        );
    }

    @Test
    public void makeSureUserAreNotEmployeeOrStudentWhenTheSameAttributeIsSetButIsEmptyValue() {

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
        user.employeeType = "vgs";

        AzureUser convertedUser = new AzureUser(user, configUser);

        assertAll(
                () -> assertEquals(user.id, convertedUser.getIdpUserObjectId()),
                () -> assertEquals(user.userPrincipalName, convertedUser.getUserPrincipalName()),
                () -> assertTrue(convertedUser.getAccountEnabled()),
                () -> assertNull(convertedUser.getStudentId()),
                () -> assertNull(convertedUser.getEmployeeId())
        );
    }

    @Test
    public void makeSureUserAreNotHandledWhenTheSameAttributeHasWrongValue() {

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
        user.employeeType = "wrong_value";

        AzureUser convertedUser = new AzureUser(user, configUser);

        assertAll(
                () -> assertEquals(user.id, convertedUser.getIdpUserObjectId()),
                () -> assertEquals(user.userPrincipalName, convertedUser.getUserPrincipalName()),
                () -> assertTrue(convertedUser.getAccountEnabled()),
                () -> assertNull(convertedUser.getEmployeeId()),
                () -> assertNull(convertedUser.getStudentId())
        );
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
        user.employeeType = "sen_ansatt";

        AzureUser convertedUser = new AzureUser(user, configUser);

        assertAll(
                () -> assertEquals(user.id, convertedUser.getIdpUserObjectId()),
                () -> assertEquals(user.userPrincipalName, convertedUser.getUserPrincipalName()),
                () -> assertTrue(convertedUser.getAccountEnabled()),
                () -> assertNull(convertedUser.getStudentId()),
                () -> assertTrue(convertedUser.getEmployeeId().contains(user.employeeId))
        );
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

        assertAll(
                () -> assertEquals(user.id, convertedUser.getIdpUserObjectId()),
                () -> assertEquals(user.userPrincipalName, convertedUser.getUserPrincipalName()),
                () -> assertTrue(convertedUser.getAccountEnabled()),
                () -> assertNull(convertedUser.getStudentId()),
                () -> assertTrue(convertedUser.getEmployeeId().contains(user.onPremisesExtensionAttributes.extensionAttribute10))
        );
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

        assertAll(
                () -> assertEquals(user.id, convertedUser.getIdpUserObjectId()),
                () -> assertEquals(user.userPrincipalName, convertedUser.getUserPrincipalName()),
                () -> assertTrue(convertedUser.getAccountEnabled()),
                () -> assertTrue(convertedUser.getStudentId().contains(user.onPremisesExtensionAttributes.extensionAttribute9)),
                () -> assertNull(convertedUser.getEmployeeId())
        );
    }
}