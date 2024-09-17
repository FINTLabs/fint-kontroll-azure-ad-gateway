package no.fintlabs.kafka;

import no.fintlabs.AzureClient;
import no.fintlabs.ConfigGroup;
import no.fintlabs.cache.FintCache;
//import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.commons.lang3.RandomStringUtils;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

//@SpringBootTest
@ExtendWith(MockitoExtension.class)
//@RunWith(SpringRunner.class)
public class ResourceGroupConsumerServiceTest {
    @Mock
    private AzureClient azureClient;

    /*@Mock
    private EntityConsumerFactoryService entityConsumerFactoryService;*/

    @Mock
    private ConfigGroup configGroup;

    @Mock
    private FintCache<String, Optional> resourceGroupCache;

    @Mock
    private Sinks.Many<Tuple2<String, Optional<ResourceGroup>>> resourceGroupSink;

    @InjectMocks
    private ResourceGroupConsumerService resourceGroupConsumerService;

    //private ResourceGroup exampleResourceGroup;

    public ResourceGroupConsumerServiceTest() {
    /*    exampleResourceGroup = ResourceGroup.builder()
                .id("123")
                .resourceId("123")
                .resourceType("licenseResource")
                .resourceName("testResourceName")
                .resourceLimit("1000")
                .build();*/
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

    private static ResourceGroup newResourceGroupFromResourceNameStatic() {
        return ResourceGroup.builder()
                .id("1234")
                .resourceId("TestKafkaKeyID")
                .displayName("TestDisplayName 12")
                .resourceName("")
                .identityProviderGroupObjectId("737e77a-8989-4444-9999-b999976c097b")
                .build();
    }

    @Test
    void makeSureEmptyQueueIsHandledOK() {
        // TODO: Implement test that can handle that a topic is empty [FKS-258]
    }

    @Test
    void processEntityGroupIsNewAndCacheIsUpdated() {
        String kafkaKeyID = "TestKafkaKeyID";

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");

        resourceGroupConsumerService.setResourceGroupSink(this.resourceGroupSink);
        resourceGroupConsumerService.processEntity(resourceGroup, kafkaKeyID);

        verify(resourceGroupCache, times(1)).put(anyString(),any());
        verify(resourceGroupSink, times(1)).tryEmitNext(any());
    }
    @Test
    void processEntityEntryAlreadyInCacheGeneratesNothing() {
        String kafkaKeyID = "TestKafkaKeyID";

        ResourceGroup resourceGroup = newResourceGroupFromResourceNameStatic();
        resourceGroupConsumerService.setResourceGroupSink(this.resourceGroupSink);

        when(resourceGroupCache.containsKey(anyString())).thenReturn(true);
        when(resourceGroupCache.get(anyString())).thenReturn(Optional.ofNullable(resourceGroup));

        resourceGroupConsumerService.processEntity(resourceGroup, kafkaKeyID);

        verify(resourceGroupCache, times(0)).put(anyString(),any());
        verify(resourceGroupSink, times(0)).tryEmitNext(any());
    }

    @Test
    void processEntity_That_Is_Empty_And_Already_In_Cache_Generates_Nothing() {
        String kafkaKeyID = "TestKafkaKeyID";

        ResourceGroup resourceGroup = null;
        resourceGroupConsumerService.setResourceGroupSink(this.resourceGroupSink);

        when(resourceGroupCache.containsKey(anyString())).thenReturn(true);
        when(resourceGroupCache.get(anyString())).thenReturn(Optional.ofNullable(resourceGroup));

        resourceGroupConsumerService.processEntity(resourceGroup, kafkaKeyID);

        verify(resourceGroupCache, times(0)).put(anyString(),any());
        verify(resourceGroupSink, times(0)).tryEmitNext(any());
    }

    @Test
    void processEntity_That_Is_Empty_ResourceGroup_But_Not_In_Cache_Continues_Operation() {
        String kafkaKeyID = "TestKafkaKeyID";

        resourceGroupConsumerService.setResourceGroupSink(this.resourceGroupSink);

        when(resourceGroupCache.containsKey(anyString())).thenReturn(false);

        resourceGroupConsumerService.processEntity(null, kafkaKeyID);

        verify(resourceGroupCache, times(1)).put(anyString(),any());
        verify(resourceGroupSink, times(1)).tryEmitNext(any());
    }

    @Test
    void updateAzure_NewGroupCallsAzureCreate() throws Exception {

        String kafkaKeyID = "TestKafkaKeyID";

        when(azureClient.doesGroupExist(anyString())).thenReturn(false);

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");
        resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.ofNullable(resourceGroup));

        verify(azureClient, times(1)).addGroupToAzure(any());
        verify(azureClient, times(0)).updateGroup(any());
        verify(azureClient, times(0)).deleteGroup(any());
    }
    @Test
    void updateAzure_UpdatedGroup_if_allowed() throws Exception {
        String kafkaKeyID = "TestKafkaKeyID";

        when(azureClient.doesGroupExist(anyString())).thenReturn(true);
        when(configGroup.getAllowgroupupdate()).thenReturn(true);

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");
        resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.ofNullable(resourceGroup));

        verify(azureClient, times(0)).addGroupToAzure(any());
        verify(azureClient, times(1)).updateGroup(any());
        verify(azureClient, times(0)).deleteGroup(any());
    }

    @Test
    void updateAzure_UpdatedGroup_if_not_allowed() throws Exception {
        String kafkaKeyID = "TestKafkaKeyID";

        when(azureClient.doesGroupExist(anyString())).thenReturn(true);
        when(configGroup.getAllowgroupupdate()).thenReturn(false);

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");
        resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.ofNullable(resourceGroup));

        verify(azureClient, times(0)).addGroupToAzure(any());
        verify(azureClient, times(0)).updateGroup(any());
        verify(azureClient, times(0)).deleteGroup(any());
    }

    @Test
    void updateAzure_DeletedGroup_If_Allowed_Calls_deleteGroup() throws Exception {
        String kafkaKeyID = "TestKafkaKeyID";

        when(configGroup.getAllowgroupdelete()).thenReturn(true);
        resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.empty());

        verify(azureClient, times(0)).addGroupToAzure(any());
        verify(azureClient, times(0)).updateGroup(any());
        verify(azureClient, times(1)).deleteGroup(any());
    }

    @Test
    void updateAzure_DeletedGroup_If_Not_Allowed_Do_Not_Calls_deleteGroup() throws Exception {
        String kafkaKeyID = "TestKafkaKeyID";

        when(configGroup.getAllowgroupdelete()).thenReturn(false);
        resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.empty());

        verify(azureClient, times(0)).addGroupToAzure(any());
        verify(azureClient, times(0)).updateGroup(any());
        verify(azureClient, times(0)).deleteGroup(any());
    }
}
