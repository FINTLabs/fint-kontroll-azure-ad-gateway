package no.fintlabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serializer.AdditionalDataManager;
import lombok.*;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.StringUtils.capitalize;

@Setter
@Getter
@RequiredArgsConstructor
public class AzureUser {


        private String id;
        private String mail;
        private String userPrincipalName;
        private String displayname;
        private String givenname;
        private String surname;
        private Object onPremisesExtensionAttributes;
        private String onPremisesUserPrincipalName;
        private String employeeIdAttribute;
        private String studentIdAttribute;

        public AzureUser(User user, ConfigUser configUser) {

                this.id = user.id;
                this.mail = user.mail;
                this.displayname = user.displayName;
                this.surname = user.surname;
                this.givenname = user.givenName;
                this.userPrincipalName = user.userPrincipalName;
                this.onPremisesUserPrincipalName = user.onPremisesUserPrincipalName;
                this.onPremisesExtensionAttributes = user.onPremisesExtensionAttributes;
                try {
                        this.employeeIdAttribute = (String) getAttributeValue(user, configUser.getEmployeeidattribute());
                } catch (Exception e) {

                }
                try {

                        this.studentIdAttribute = (String) getAttributeValue(user, configUser.getStudentidattribute());
                } catch (Exception e) {

                }

        }


        public static Object getAttributeValue(User user, String attributeName) throws NoSuchFieldException, IllegalAccessException {
                // Split the attribute name by dot to get the nested field names
                String[] attributeParts = attributeName.split("\\.");

                if (attributeParts[0].equals("onPremisesExtensionAttributes")) {
                        OnPremisesExtensionAttributes attributeValues = user.onPremisesExtensionAttributes;
                        Field[] fields = OnPremisesExtensionAttributes.class.getDeclaredFields();
                        for (Field field : fields) {
                                // make the field accessible to be able to read its value
                                field.setAccessible(true);
                                try {
                                        // get the value of the field from the group object
                                        Object value = field.get(attributeValues);

                                        // add the value to the results map if it's not null
                                        if (value != null && field.getName().startsWith("extensionAttributes")) {
                                                return value;
                                        }
                                } catch (IllegalAccessException e) {
                                        // handle the exception if the field is not accessible
                                        e.printStackTrace();
                                }
                        }
                }
                else
                {
                        Field[] fields = User.class.getDeclaredFields();
                        for (Field field : fields) {
                                // make the field accessible to be able to read its value
                                field.setAccessible(true);
                                try {
                                        // get the value of the field from the group object
                                        Object value = field.get(user);

                                        // add the value to the results map if it's not null
                                        if (value != null && !field.getName().startsWith("additionalDataManager") && !field.getName().startsWith("onPremisesExtensionAttributes")) {
                                                return value;
                                        }
                                } catch (IllegalAccessException e) {
                                        // handle the exception if the field is not accessible
                                        e.printStackTrace();
                                }
                        }
                }

                // Return the attribute value
                return null;
        }


}




