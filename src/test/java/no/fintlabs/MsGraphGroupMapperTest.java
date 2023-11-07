package no.fintlabs;


import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.Group;
import no.fintlabs.kafka.ResourceGroup;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.event.annotation.BeforeTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MsGraphGroupMapperTest {

    @Mock
    private ConfigGroup configGroup;

    @Mock
    private Config config;

    private ResourceGroup resourceGroup;

    public MsGraphGroupMapperTest() {
        resourceGroup = ResourceGroup.builder()
                .id("123")
                .resourceId("123")
                .resourceType("licenseResource")
                .resourceName("testResourceName")
                .resourceLimit("1000")
                .build();
    }

    @BeforeAll
    static void setup() {
    }

    @Test
    public void shouldMapFromResourceGroupToMsGraphGroup() {
        String fintKontrollIdAttribute = "fakeFintKontrollId";
        String resourceId = RandomStringUtils.random(4);

        when(configGroup.getPrefix()).thenReturn("afk-");
        when(configGroup.getSuffix()).thenReturn("-agg-kon");
        when(configGroup.getFintkontrollidattribute()).thenReturn(fintKontrollIdAttribute);

        ResourceGroup group = ResourceGroup.builder()
                .id(RandomStringUtils.random(6))
                .identityProviderGroupObjectId(RandomStringUtils.random(4))
                .displayName("TestDisplayName " + RandomStringUtils.random(6))
                .resourceId(resourceId)
                .resourceName("Adobe Cloud")
                .resourceType("licenceResource")
                .build();

        Group msGroup = new MsGraphGroupMapper().toMsGraphGroup(group, configGroup, config);

        assertThat(msGroup.displayName).isEqualTo("afk-lic-adobe.cloud-agg-kon");
        assertThat(msGroup.mailEnabled).isFalse();
        assertThat(msGroup.securityEnabled).isTrue();
        assertThat(msGroup.mailNickname).isEqualTo("adobecloud");
        assertThat(msGroup.additionalDataManager().get(fintKontrollIdAttribute).getAsString()).isEqualTo(resourceId);
    }

    @Test
    public void displayNameShouldTransformToLowecase() {
        when(configGroup.getPrefix()).thenReturn("Bfk-");
        when(configGroup.getSuffix()).thenReturn("-AgG-kon");
        ResourceGroup group = resourceGroup.toBuilder()
                .resourceName("ArDoQ")
                .resourceType("ApplicationResource")
                .build();
        Group msGroup = new MsGraphGroupMapper().toMsGraphGroup(group, configGroup, config);
        assertThat(msGroup.displayName).isEqualTo("bfk-app-ardoq-agg-kon");
    }

    @Test
    public void displayNameShouldTransformSpacesToDots() {
        when(configGroup.getPrefix()).thenReturn("OfK-");
        when(configGroup.getSuffix()).thenReturn("-kon");
        ResourceGroup group = resourceGroup.toBuilder()
                .resourceName("QLikSense FK Inntak")
                .resourceType("RoLEesouRCE")
                .build();
        Group msGroup = new MsGraphGroupMapper().toMsGraphGroup(group, configGroup, config);
        assertThat(msGroup.displayName).isEqualTo("ofk-rol-qliksense.fk.inntak-kon");
    }

    @Test
    public void mailNickNameShouldRemoveSpecialCharacters() {
        ResourceGroup group = resourceGroup.toBuilder()
                .resourceName("Adobe Cloud 123")
                .build();
        Group msGroup = new MsGraphGroupMapper().toMsGraphGroup(group, configGroup, config);
        assertThat(msGroup.mailNickname).isEqualTo("adobecloud123");
    }

    @Test
    public void mailNickNameShouldNeverExtend64Characters() {
        ResourceGroup group = resourceGroup.toBuilder()
                .resourceName("Adobe Cloud 123 test adfasdfkasdf awe fawfe awdf aw dfwodf poaw eporwkfæpo daof wpeofæap awe w")
                .build();
        Group msGroup = new MsGraphGroupMapper().toMsGraphGroup(group, configGroup, config);
        assertThat(msGroup.mailNickname.length()).isEqualTo(64);
    }

    @Test
    public void mailNickNameShouldRemoveSpecialCharactersAndClipLength() {
        ResourceGroup group = resourceGroup.toBuilder()
                .resourceName("Adobe_Cloud-123@$%&$€£... test adfasdfkasdf awe fawfe awdf aw dfwodf poaw eporwkfæpo daof wpeofæap awe w")
                .build();
        Group msGroup = new MsGraphGroupMapper().toMsGraphGroup(group, configGroup, config);
        assertThat(msGroup.mailNickname).isEqualTo("adobecloud123testadfasdfkasdfawefawfeawdfawdfwodfpoaweporwkfpoda");
    }


//https://fintlabs.atlassian.net/wiki/spaces/FINTKB/pages/693403649/Navngiving+p+Azure+AD-grupper+fra+fint-kontroll
}