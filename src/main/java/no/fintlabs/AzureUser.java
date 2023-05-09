package no.fintlabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonElement;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serializer.AdditionalDataManager;
import lombok.*;
import lombok.extern.log4j.Log4j2;
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
@NoArgsConstructor
//@RequiredArgsConstructor
@Log4j2
public class AzureUser {


        private String id;
        private String mail;
        private String userPrincipalName;
        /*private String displayname;
        private String givenname;
        private String surname;
        private Object onPremisesExtensionAttributes;
        private String onPremisesUserPrincipalName;*/
        private String employeeId;
        private String studentId;

        public AzureUser(User user, ConfigUser configUser) {

                this.id = user.id;
                this.mail = user.mail;
                /*this.displayname = user.displayName;
                this.surname = user.surname;
                this.givenname = user.givenName;*/
                this.userPrincipalName = user.userPrincipalName;
                //this.onPremisesUserPrincipalName = user.onPremisesUserPrincipalName;
                //this.onPremisesExtensionAttributes = user.onPremisesExtensionAttributes;
                try {
                        this.employeeId = getAttributeValue(user, configUser.getEmployeeidattribute());
                        this.studentId = getAttributeValue(user, configUser.getStudentidattribute());
                } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                }
        }

        public String getAttributeValue(User user, String attributeName) throws NoSuchFieldException {
                // Split the attribute name by dot to get the nested field names
                String[] attributeParts = attributeName.split("\\.");
                if (attributeParts[0].equals("onPremisesExtensionAttributes")) {
                        JsonElement oa = user.additionalDataManager().get("onPremisesExtensionAttributes");
                        if (user.onPremisesExtensionAttributes == null){
                                log.info("OnPremisesExtensionAttribute property is null");
                                return null;
                        }
                        Field extField = user.onPremisesExtensionAttributes.getClass().getField(attributeParts[1]);
                        /*Object o = extField.get(onPremisesExtensionAttributes);
                        return o;*/
                        String ran = "true";
                        //OnPremisesExtensionAttributes attributeValues = user.onPremisesExtensionAttributes;
                        //Field[] fields = OnPremisesExtensionAttributes.class.getDeclaredFields();
                        //Field extField = OnPremisesExtensionAttributes.class.getDeclaredField(attributeParts[1]);
                        //Field extField = attributeValues.getClass().getDeclaredField(attributeParts[1]);
                        //Field extField = user.onPremisesExtensionAttributes.getClass().getDeclaredField(attributeParts[1]);

                        //extField.setAccessible(true);
                        //return extField.get(attributeValues);
                        return "Yess";
                }
                return null;
        }
                        /*for (Field field : fields) {
                                // make the field accessible to be able to read its value
                                field.setAccessible(true);
                                try {
                                        // get the value of the field from the group object
                                        Object value = field.get(attributeValues);

                                        // add the value to the results map if it's not null
                                        if (value != null && field.getName().startsWith("extensionattribute")) {
                                                return value;
                                        }
                                } catch (IllegalAccessException e) {
                                        // handle the exception if the field is not accessible
                                        e.printStackTrace();
                                }
                        }*/
                //}
                /*else
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
        }*/
}




