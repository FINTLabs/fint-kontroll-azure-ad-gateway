package no.fintlabs.azure;

import com.microsoft.graph.models.OnPremisesExtensionAttributes;
import com.microsoft.graph.models.User;
import no.fintlabs.config.ConfigUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureUserTest {
    @Mock
    private ConfigUser configUser;

    @Mock
    private ConcurrentHashMap<String, AzureUser> entraIdUserCache;

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

        assertAll(
                () -> assertEquals(convertedUser.getId(), user.getId()),
                () -> assertEquals(convertedUser.getIdpUserObjectId(), user.getId()),
                () -> assertEquals(convertedUser.getUserPrincipalName(), user.getUserPrincipalName()),
                () -> assertTrue(convertedUser.getAccountEnabled())
        );
    }

    @Test
    public void makeSureExternalUserAttributeAccountEnabledIsAccountedFor() {


        User user = new User();
        user.setId("123");
        user.setMail("testuser@mail.com");
        user.setUserPrincipalName("testuser@mail.com");
        user.setAccountEnabled(true);

        AzureUserExternal convertedUser = new AzureUserExternal(user, configUser);

        assertAll(
                () -> assertEquals(convertedUser.getIdpUserObjectId(), user.getId()),
                () -> assertEquals(convertedUser.getUserPrincipalName(), user.getUserPrincipalName()),
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
        user.setId("123");
        user.setMail("testuser@mail.com");
        user.setUserPrincipalName("testuser@mail.com");
        user.setAccountEnabled(true);
        user.setEmployeeId("123");
        user.setEmployeeType("elev");

        AzureUser convertedUser = new AzureUser(user, configUser);

        assertAll(
                () -> assertEquals(convertedUser.getIdpUserObjectId(), user.getId()),
                () -> assertEquals(convertedUser.getUserPrincipalName(), user.getUserPrincipalName()),
                () -> assertTrue(convertedUser.getAccountEnabled()),
                () -> assertEquals(convertedUser.getStudentId(), user.getEmployeeId()),
                () -> assertNull(convertedUser.getEmployeeId())
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
        user.setId("123");
        user.setMail("testuser@mail.com");
        user.setUserPrincipalName("testuser@mail.com");
        user.setAccountEnabled(true);
        user.setEmployeeId("123");
        user.setEmployeeType("ansatt");

        AzureUser convertedUser = new AzureUser(user, configUser);

        assertAll(
                () -> assertEquals(convertedUser.getIdpUserObjectId(), user.getId()),
                () -> assertEquals(convertedUser.getUserPrincipalName(), user.getUserPrincipalName()),
                () -> assertTrue(convertedUser.getAccountEnabled()),
                () -> assertNull(convertedUser.getStudentId()),
                () -> assertEquals(convertedUser.getEmployeeId(), user.getEmployeeId())
        );

    }

    @Test
    public void makeSureUserAreHandledAsEmployeeWhenTheSameAttributeIsNOTSet() {

        when(configUser.getUseSameIdNumAttribute()).thenReturn(false);
        when(configUser.getEmployeeidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute10");
        when(configUser.getStudentidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute9");

        User user = new User();
        user.setId("123");
        user.setMail("testuser@mail.com");
        user.setUserPrincipalName("testuser@mail.com");
        user.setAccountEnabled(true);
        user.setOnPremisesExtensionAttributes(new OnPremisesExtensionAttributes());
        user.getOnPremisesExtensionAttributes().setExtensionAttribute10("432");

        AzureUser convertedUser = new AzureUser(user, configUser);

        assertAll(
                () -> assertEquals(convertedUser.getIdpUserObjectId(), user.getId()),
                () -> assertEquals(convertedUser.getUserPrincipalName(), user.getUserPrincipalName()),
                () -> assertTrue(convertedUser.getAccountEnabled()),
                () -> assertNull(convertedUser.getStudentId()),
                () -> assertEquals(convertedUser.getEmployeeId(), user.getOnPremisesExtensionAttributes().getExtensionAttribute10())
        );
    }

    @Test
    public void makeSureUserAreHandledAsStudentWhenTheSameAttributeIsNOTSet() {

        when(configUser.getUseSameIdNumAttribute()).thenReturn(false);
        when(configUser.getEmployeeidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute10");
        when(configUser.getStudentidattribute()).thenReturn("onPremisesExtensionAttributes.extensionAttribute9");

        User user = new User();
        user.setMail("testuser@mail.com");
        user.setUserPrincipalName("testuser@mail.com");
        user.setAccountEnabled(true);
        user.setOnPremisesExtensionAttributes(new OnPremisesExtensionAttributes());
        user.getOnPremisesExtensionAttributes().setExtensionAttribute9("123");


        AzureUser convertedUser = new AzureUser(user, configUser);

        assertAll(
                () -> assertEquals(convertedUser.getIdpUserObjectId(), user.getId()),
                () -> assertEquals(convertedUser.getUserPrincipalName(), user.getUserPrincipalName()),
                () -> assertTrue(convertedUser.getAccountEnabled()),
                () -> assertEquals(convertedUser.getStudentId(), user.getOnPremisesExtensionAttributes().getExtensionAttribute9()),
                () -> assertNull(convertedUser.getEmployeeId())
        );
    }
}