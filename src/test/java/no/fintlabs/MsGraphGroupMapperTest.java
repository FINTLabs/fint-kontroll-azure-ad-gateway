package no.fintlabs;

import com.microsoft.graph.models.Group;
import no.fintlabs.kafka.ResourceGroup;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import static org.assertj.core.api.Assertions.*;
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
                .resourceId("resource123")
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
        //String resourceId = RandomStringUtils.random(4);
        String Id = RandomStringUtils.randomNumeric(8);

        when(configGroup.getPrefix()).thenReturn("afk-");
        when(configGroup.getSuffix()).thenReturn("-agg-kon");
        when(configGroup.getFintkontrollidattribute()).thenReturn(fintKontrollIdAttribute);

        ResourceGroup group = ResourceGroup.builder()
                .id(Id)
                .identityProviderGroupObjectId(RandomStringUtils.random(4))
                .displayName("TestDisplayName " + RandomStringUtils.random(6))
                .resourceId("TestResourceID" + RandomStringUtils.randomAlphanumeric(6))
                .resourceName("Adobe Cloud")
                .resourceType("licenceResource")
                .build();

        Group msGroup = new MsGraphGroupMapper().toMsGraphGroup(group, configGroup, config);

        assertThat(msGroup.displayName).isEqualTo("afk-lic-adobe.cloud-agg-kon");
        assertThat(msGroup.mailEnabled).isFalse();
        assertThat(msGroup.securityEnabled).isTrue();
        assertThat(msGroup.mailNickname).isEqualTo("adobecloud");
        assertThat(msGroup.additionalDataManager().get(fintKontrollIdAttribute).getAsString()).isEqualTo(Id);
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

    @Test
    void shouldMapFromResourceGroupToMsGraphGroup_withOnlyPrefixSet() {
        String resourceGroupIdAttribute =
                "extension_" + UUID.randomUUID().toString().replace("-", "") + "_ResourceGroupID";
        String strUuid = UUID.randomUUID().toString();
        String id = String.valueOf(ThreadLocalRandom.current()
                .nextInt(10_000_000, 100_000_000));
        String resourceName = "Test-thomas-fintkontroll-09.12.25-2";
        String prefix = "FINT-";
        String suffix = "-suff";

        when(configGroup.getPrefix()).thenReturn(prefix);
        //when(configGroup.getSuffix()).thenReturn(null);
        when(configGroup.getFintkontrollidattribute()).thenReturn(resourceGroupIdAttribute);

        ResourceGroup group = ResourceGroup.builder()
                .id(id)
                .displayName("TestDisplayName " + RandomStringUtils.insecure().nextAlphabetic(6))
                .resourceId(strUuid)
                .resourceName(resourceName)
                .resourceType("ApplicationResource")
                .build();

        Group msGroup = new MsGraphGroupMapper().toMsGraphGroup(group, configGroup, config);

        String expectedCore =
                group.getResourceType().substring(0, 3) + "-" + group.getResourceName().replaceAll("\\s+", ".");
        String expectedDisplayName = (prefix + expectedCore).toLowerCase();

        assertThat(msGroup.displayName).isEqualTo(expectedDisplayName);
        assertThat(msGroup.mailEnabled).isFalse();
        assertThat(msGroup.securityEnabled).isTrue();
        assertThat(msGroup.mailNickname).isEqualTo(resourceName.replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase());
        assertThat(msGroup.additionalDataManager()
                .get(resourceGroupIdAttribute)
                .getAsString())
                .isEqualTo(id);
    }

    @Test
    void shouldMapFromResourceGroupToMsGraphGroup_withOnlySuffixSet() {
        String resourceGroupIdAttribute =
                "extension_" + UUID.randomUUID().toString().replace("-", "") + "_ResourceGroupID";
        String strUuid = UUID.randomUUID().toString();
        String id = String.valueOf(ThreadLocalRandom.current()
                .nextInt(10_000_000, 100_000_000));
        String resourceName = "Test-thomas-fintkontroll-09.12.25-2";
        String prefix = "FINT-";
        String suffix = "-suff";

        //when(configGroup.getPrefix()).thenReturn(null);
        when(configGroup.getSuffix()).thenReturn(suffix);
        when(configGroup.getFintkontrollidattribute()).thenReturn(resourceGroupIdAttribute);

        ResourceGroup group = ResourceGroup.builder()
                .id(id)
                .displayName("TestDisplayName " + RandomStringUtils.insecure().nextAlphabetic(6))
                .resourceId(strUuid)
                .resourceName(resourceName)
                .resourceType("ApplicationResource")
                .build();

        Group msGroup = new MsGraphGroupMapper().toMsGraphGroup(group, configGroup, config);

        String expectedCore =
                group.getResourceType().substring(0, 3) + "-" + group.getResourceName().replaceAll("\\s+", ".");
        String expectedDisplayName = (expectedCore).toLowerCase() + suffix;

        assertThat(msGroup.displayName).isEqualTo(expectedDisplayName);
        assertThat(msGroup.mailEnabled).isFalse();
        assertThat(msGroup.securityEnabled).isTrue();
        assertThat(msGroup.mailNickname).isEqualTo(resourceName.replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase());
        assertThat(msGroup.additionalDataManager()
                .get(resourceGroupIdAttribute)
                .getAsString())
                .isEqualTo(id);
    }

//https://fintlabs.atlassian.net/wiki/spaces/FINTKB/pages/693403649/Navngiving+p+Azure+AD-grupper+fra+fint-kontroll
}