package no.fintlabs;


import com.microsoft.graph.models.Group;
import no.fintlabs.kafka.ResourceGroup;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MsGraphGroupMapperTest {

    @Mock
    private ConfigGroup configGroup;

    @Mock
    private Config config;

    @Test
    public void shouldMapFromResourceGroupToMsGraphGroup() {
        String fintKontrollIdAttribute = "fakeFintKontrollId";

        when(configGroup.getPrefix()).thenReturn("afk-");
        when(configGroup.getSuffix()).thenReturn("-agg-kon");
        when(configGroup.getFintkontrollidattribute()).thenReturn(fintKontrollIdAttribute);

        String resourceGroupId = RandomStringUtils.random(4);

        ResourceGroup group = new ResourceGroup(
                resourceGroupId,
                "testResourceID" + RandomStringUtils.random(4),
                "TestDisplayName " + RandomStringUtils.random(6),
                RandomStringUtils.random(12),
                "Adobe Cloud",
                "licenceResource",
                "1000");

        Group msGroup = new MsGraphGroupMapper().toMsGraphGroup(group, configGroup, config);

        assertThat(msGroup.displayName).isEqualTo("afk-lic-adobe.cloud-agg-kon");
        assertThat(msGroup.mailEnabled).isFalse();
        assertThat(msGroup.securityEnabled).isTrue();
        assertThat(msGroup.mailNickname).isEqualTo("AdobeCloud");
        assertThat(msGroup.additionalDataManager().get(fintKontrollIdAttribute).getAsString()).isEqualTo(resourceGroupId);
    }


}

//https://fintlabs.atlassian.net/wiki/spaces/FINTKB/pages/693403649/Navngiving+p+Azure+AD-grupper+fra+fint-kontroll
