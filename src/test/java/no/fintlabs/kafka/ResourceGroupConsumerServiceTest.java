package no.fintlabs.kafka;

import com.microsoft.graph.models.Group;
import no.fintlabs.AzureClient;
import no.fintlabs.ConfigGroup;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.azure.AzureGroupProducerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.commons.lang3.RandomStringUtils;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

//@SpringBootTest
@ExtendWith(MockitoExtension.class)
//@RunWith(SpringRunner.class)
public class ResourceGroupConsumerServiceTest {
    @Mock
    private AzureClient azureClient;

    @Mock
    private Group group;

    @Mock
    private AzureGroupProducerService azureGroupProducerService;

    /*@Mock
    private EntityConsumerFactoryService entityConsumerFactoryService;*/

    @Mock
    private ConfigGroup configGroup;

    @Mock
    private ConcurrentHashMap<String, Optional<ResourceGroup>> resourceGroupCache;

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
        //when(resourceGroupSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);
        resourceGroupConsumerService.setResourceGroupSink(this.resourceGroupSink);
        resourceGroupConsumerService.processEntity(resourceGroup, kafkaKeyID);


        verify(resourceGroupCache, times(1)).put(anyString(),any());
        verify(resourceGroupSink, times(1)).emitNext(any(),any());
    }

    @Test
    void processEntityEntryAlreadyInCacheStillProcesses() {
        String kafkaKeyID = "TestKafkaKeyID";
        ResourceGroup resourceGroup = newResourceGroupFromResourceNameStatic();

        resourceGroupConsumerService.setResourceGroupSink(this.resourceGroupSink);

        //when(resourceGroupSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);
        when(resourceGroupCache.containsKey(anyString())).thenReturn(true);
        when(resourceGroupCache.get(anyString())).thenReturn(Optional.ofNullable(resourceGroup));

        resourceGroupConsumerService.processEntity(resourceGroup, kafkaKeyID);

        verify(resourceGroupCache, times(0)).put(anyString(), any());
        verify(resourceGroupSink, times(1)).emitNext(any(),any());
    }

    @Test
    void processEntity_That_Is_Empty_And_Already_In_Cache_Still_Processes() {
        String kafkaKeyID = "TestKafkaKeyID";
        ResourceGroup resourceGroup = null;

        resourceGroupConsumerService.setResourceGroupSink(this.resourceGroupSink);

        //when(resourceGroupSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        resourceGroupConsumerService.processEntity(resourceGroup, kafkaKeyID);

        verify(resourceGroupCache, times(1)).put(eq(kafkaKeyID), any());
        verify(resourceGroupSink, times(1)).emitNext(any(),any());

    }

    @Test
    void processEntityEntryAlreadyInCacheGeneratesNewKafkaMessage() {
        String kafkaKeyID = "TestKafkaKeyID";
        ResourceGroup resourceGroup = newResourceGroupFromResourceNameStatic();


        resourceGroupConsumerService.setResourceGroupSink(this.resourceGroupSink);
        //when(resourceGroupSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        resourceGroupConsumerService.processEntity(resourceGroup, kafkaKeyID);

        verify(resourceGroupCache, times(1)).put(eq(kafkaKeyID), any());
        verify(resourceGroupSink, times(1)).emitNext(any(),any());
    }

    @Test
    void processEntity_That_Is_Empty_And_Already_In_Cache_Generates_Nothing() {
        String kafkaKeyID = "TestKafkaKeyID";

        resourceGroupConsumerService.setResourceGroupSink(this.resourceGroupSink);
        //when(resourceGroupSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);
        when(resourceGroupCache.containsKey(anyString())).thenReturn(true);
        when(resourceGroupCache.get(anyString())).thenReturn(Optional.empty());

        resourceGroupConsumerService.processEntity(null, kafkaKeyID);

        verify(resourceGroupCache, times(0)).put(anyString(),any());
        verify(resourceGroupSink, times(1)).emitNext(any(),any());

    }

    @Test
    void processEntity_That_Is_Empty_ResourceGroup_But_Not_In_Cache_Continues_Operation() {
        String kafkaKeyID = "TestKafkaKeyID";

        resourceGroupConsumerService.setResourceGroupSink(this.resourceGroupSink);
        //when(resourceGroupSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        when(resourceGroupCache.containsKey(anyString())).thenReturn(false);

        resourceGroupConsumerService.processEntity(null, kafkaKeyID);

        verify(resourceGroupCache, times(1)).put(anyString(),any());
        verify(resourceGroupSink, times(1)).emitNext(any(),any());
    }

    @Test
    void updateAzure_NewGroupCallsAzureCreate() {

        String kafkaKeyID = "TestKafkaKeyID";

        when(azureClient.doesGroupExist(anyString())).thenReturn(false);

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");
        resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.ofNullable(resourceGroup));

        verify(azureClient, times(1)).addGroupToAzure(any());
        verify(azureClient, times(0)).updateGroupAsync(any());
        verify(azureClient, times(0)).deleteGroup(any());
    }
    @Test
    void updateAzure_UpdatedGroup_if_allowed() {
        String kafkaKeyID = "TestKafkaKeyID";

        when(azureClient.doesGroupExist(anyString())).thenReturn(true);
        when(configGroup.getAllowgroupupdate()).thenReturn(true);

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");
        resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.ofNullable(resourceGroup));

        verify(azureClient, times(0)).addGroupToAzure(any());
        verify(azureClient, times(1)).updateGroupAsync(any());
        verify(azureClient, times(0)).deleteGroup(any());
    }

    @Test
    void updateAzure_UpdatedGroup_if_not_allowed() {
        String kafkaKeyID = "TestKafkaKeyID";

        when(azureClient.doesGroupExist(anyString())).thenReturn(true);
        when(configGroup.getAllowgroupupdate()).thenReturn(false);

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");
        resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.ofNullable(resourceGroup));

        verify(azureClient, times(0)).addGroupToAzure(any());
        verify(azureClient, times(0)).updateGroupAsync(any());
        verify(azureClient, times(0)).deleteGroup(any());
    }

    @Test
    void updateAzure_DeletedGroup_If_Allowed_Calls_deleteGroup() {
        String kafkaKeyID = "TestKafkaKeyID";

        when(configGroup.getAllowgroupdelete()).thenReturn(true);
        resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.empty());

        verify(azureClient, times(0)).addGroupToAzure(any());
        verify(azureClient, times(0)).updateGroupAsync(any());
        verify(azureClient, times(1)).deleteGroupAsync(any());
    }

    @Test
    void updateAzure_DeletedGroup_If_Not_Allowed_Do_Not_Calls_deleteGroup() {
        String kafkaKeyID = "TestKafkaKeyID";

        when(configGroup.getAllowgroupdelete()).thenReturn(false);
        resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.empty());

        verify(azureClient, times(0)).addGroupToAzure(any());
        verify(azureClient, times(0)).updateGroupAsync(any());
        verify(azureClient, times(0)).deleteGroup(any());
    }

    @Test
    void kafkaMessage_newGroup_shouldCallAddGroupToAzure() {
        // given
        String key = "k1";
        ResourceGroup rg = ResourceGroup.builder()
                .id("1")
                .resourceName("Test-thomas-fintkontroll-09.12.25-2")
                .identityProviderGroupObjectId(null)
                .build();

        when(azureClient.doesGroupExist("1")).thenReturn(false);

        resourceGroupConsumerService.processEntity(rg, key);

        verify(azureClient, timeout(500)).addGroupToAzure(rg);
        verify(azureClient, never()).updateGroupAsync(any());
        verify(azureClient, never()).deleteGroupAsync(any());
    }
}
