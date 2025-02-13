package no.fintlabs.azure;


import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.Group;

import no.fintlabs.ConfigGroup;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AzureGroupTest {
    @Mock
    private ConfigGroup configGroup;
    //public AzureGroup(Group group, ConfigGroup configGroup) {
    @Test
    public void makeSureConversionToLongFromRandomStringThrowsException() {
        Group testgroup = new Group();
        testgroup.id = RandomStringUtils.randomNumeric(8);
        testgroup.displayName = "-pre-app-microsoft.kabal-suff-";
        testgroup.additionalDataManager().put(configGroup.getFintkontrollidattribute(), new JsonPrimitive("kabal"));

        assertThrows(NumberFormatException.class, () -> {
            AzureGroup newTestgroup = new AzureGroup(testgroup, configGroup);
        });
    }
    @Test
    public void makeSureNumericStringIsConvertedCorrectly() {
        Group testgroup = new Group();
        testgroup.id = RandomStringUtils.randomNumeric(8);
        testgroup.displayName = "-pre-app-microsoft.kabal-suff-";
        String number = RandomStringUtils.randomNumeric(8);

        testgroup.additionalDataManager().put(configGroup.getFintkontrollidattribute(), new JsonPrimitive(number));

        AzureGroup newTestGroup = new AzureGroup(testgroup, configGroup);

        assertThat(newTestGroup.getResourceGroupID() == Long.valueOf(number));
    }
}