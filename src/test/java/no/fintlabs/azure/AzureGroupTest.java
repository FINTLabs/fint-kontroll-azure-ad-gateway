package no.fintlabs.azure;


import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.Group;

import no.fintlabs.ConfigGroup;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AzureGroupTest {
    @Mock
    private ConfigGroup configGroup;
    @Test
    public void makeSureConversionToLongFromRandomStringThrowsException() {
        Group testgroup = new Group();
        testgroup.setId(RandomStringUtils.randomNumeric(8));
        testgroup.setDisplayName("-pre-app-microsoft.kabal-suff-");
        HashMap<String, Object> additionalData = new HashMap<>();
        additionalData.put(configGroup.getFintkontrollidattribute(), "kabal");
        testgroup.setAdditionalData(additionalData);

        assertThrows(NumberFormatException.class, () -> {
            AzureGroup newTestgroup = new AzureGroup(testgroup, configGroup);
        });
    }
    @Test
    public void makeSureNumericStringIsConvertedCorrectly() {
        Group testgroup = new Group();
        testgroup.setId(RandomStringUtils.randomNumeric(8));
        testgroup.setDisplayName("-pre-app-microsoft.kabal-suff-");
        String number = RandomStringUtils.randomNumeric(8);
        HashMap<String, Object> additionalData = new HashMap<>();
        additionalData.put(configGroup.getFintkontrollidattribute(), number);
        testgroup.setAdditionalData(additionalData);
        AzureGroup newTestGroup = new AzureGroup(testgroup, configGroup);

        assertThat(newTestGroup.getResourceGroupID() == Long.valueOf(number));
    }
}