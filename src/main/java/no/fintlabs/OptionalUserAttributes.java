package no.fintlabs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class OptionalUserAttributes {

    @Value("fint.flyt.azure-ad-gateway")
    private List<String> optionalUserAttributes;

    public List<String> getAllUserAttributes() {
        List<String> allUserAttributes = new ArrayList<>();
        allUserAttributes.addAll(Arrays.asList(AzureUser.requiredAttributes));

        if (optionalUserAttributes != null && !optionalUserAttributes.isEmpty()) {
            allUserAttributes.addAll(optionalUserAttributes);
        }

        return allUserAttributes;
    }
}