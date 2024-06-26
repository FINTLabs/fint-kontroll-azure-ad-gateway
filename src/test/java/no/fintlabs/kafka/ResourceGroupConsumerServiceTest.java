package no.fintlabs.kafka;

import no.fintlabs.AzureClient;
import no.fintlabs.ConfigGroup;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.commons.lang3.RandomStringUtils;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

//@SpringBootTest
@ExtendWith(MockitoExtension.class)
//@RunWith(SpringRunner.class)
public class ResourceGroupConsumerServiceTest {
    @Mock
    private AzureClient azureClient;

    @Mock
    private EntityConsumerFactoryService entityConsumerFactoryService;

    @Mock
    private ConfigGroup configGroup;

    @Mock
    private FintCache<String, ResourceGroup> resourceGroupCache;

    @InjectMocks
    private ResourceGroupConsumerService resourceGroupConsumerService;

    private ResourceGroup exampleResourceGroup;

    public ResourceGroupConsumerServiceTest() {
        exampleResourceGroup = ResourceGroup.builder()
                .id("123")
                .resourceId("123")
                .resourceType("licenseResource")
                .resourceName("testResourceName")
                .resourceLimit("1000")
                .build();
    }

    ResourceGroup newResourceGroupFromResourceName(String inResourceName) {
        return ResourceGroup.builder()
                .id(RandomStringUtils.random(4))
                .resourceId(RandomStringUtils.randomAlphanumeric(12))
                .displayName("TestDisplayName " + RandomStringUtils.random(6))
                .resourceId(RandomStringUtils.random(12))
                .resourceName(inResourceName)
                .identityProviderGroupObjectId(RandomStringUtils.random(12))
                .build();
    }

    @Test
    void makeSureEmptyQueueIsHandledOK() {
        // TODO: Implement test that can handle that a topic is empty [FKS-258]
    }

    @Test
    void processEntityNewGroupGetsCallsAzureCreate() {

        String kafkaKeyID = "TestKafkaKeyID";

        when(azureClient.doesGroupExist(anyString())).thenReturn(false);

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");
        resourceGroupConsumerService.processEntity(resourceGroup, kafkaKeyID);

        verify(azureClient, times(1)).addGroupToAzure(any());
        verify(azureClient, times(0)).updateGroup(any());
        verify(azureClient, times(0)).deleteGroup(any());
        verify(resourceGroupCache, times(1)).put(anyString(), any());
    }

    @Test
    void processEntityUpdatedGroupGetsCallsAzureCreate() {
        String kafkaKeyID = "TestKafkaKeyID";

        when(azureClient.doesGroupExist(anyString())).thenReturn(true);

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");
        resourceGroupConsumerService.processEntity(resourceGroup, kafkaKeyID);

        verify(azureClient, times(0)).addGroupToAzure(any());
        verify(azureClient, times(1)).updateGroup(any());
        verify(azureClient, times(0)).deleteGroup(any());
    }

}
