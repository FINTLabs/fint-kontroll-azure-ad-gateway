package no.fintlabs.core;

import com.microsoft.graph.models.Group;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.config.ConfigGroup;
import no.fintlabs.core.entity.CoreGroup;
import no.fintlabs.kafka.ResourceGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class CoreGroupMapperTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @Disabled
    void verifyResourceGroupAdnAzureGroupResultsInIdenticalHash() {
        ResourceGroup resourceGroup = new ResourceGroup(
                "1",
                "testdisplayname",
                "9o283hfaliudfhlas",
                "testresource",
                "Application"
        );
        Group MSGraphGroup = new Group();
        AzureGroup azureGroup = new AzureGroup( MSGraphGroup, ConfigGroup.builder().build() );
        CoreGroup group1 = CoreGroupMapper.toCoreGroup(resourceGroup, ConfigGroup.builder().build());
        CoreGroup group2 = CoreGroupMapper.toCoreGroup(azureGroup);

        assertEquals(group1.getChecksum(), group2.getChecksum());
    }
}