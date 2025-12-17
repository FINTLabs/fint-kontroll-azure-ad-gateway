package no.fintlabs.kafka;

import no.fintlabs.config.Config;
import no.fintlabs.config.ConfigGroup;
import no.fintlabs.core.CoreObjectListOrchestrator;
import no.fintlabs.group.MsGraphGroup;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.core.publisher.Flux;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "deprecation"})
@ExtendWith(MockitoExtension.class)
public class ResourceGroupConsumerServiceTest {
    @Mock
    private MsGraphGroup msGraphGroup;

    @Mock
    private ConfigGroup configGroup;

    @Mock
    private CoreObjectListOrchestrator coreObjectListOrchestrator;

    @Mock
    private ConcurrentHashMap<String, Optional<ResourceGroup>> resourceGroupCache;

    @Mock
    private Sinks.Many<Tuple2<String, Optional<ResourceGroup>>> resourceGroupSink;



    @InjectMocks
    private ResourceGroupConsumerService resourceGroupConsumerService;

    ResourceGroup newResourceGroupFromResourceName(String inResourceName) {
        return ResourceGroup.builder()
                .id(RandomStringUtils.random(4))
                .displayName("TestDisplayName " + RandomStringUtils.random(6))
                .resourceName(inResourceName)
                .identityProviderGroupObjectId(RandomStringUtils.random(12))
                .build();
    }

    private static ResourceGroup newResourceGroupFromResourceNameStatic() {
        return ResourceGroup.builder()
                .id("1234")
                .displayName("TestDisplayName 12")
                .resourceName("")
                .identityProviderGroupObjectId("737e77a-8989-4444-9999-b999976c097b")
                .build();
    }

    @Disabled
    @Test
    void makeSureEmptyQueueIsHandledOK() {
        // TODO: Implement test that can handle that a topic is empty [FKS-258]
    }

    // TODO: Fix after batching has been reimplemented
    @Disabled
    @Test
    void processEntityGroupIsNewAndCacheIsUpdated() {
        String kafkaKeyID = "TestKafkaKeyID";
        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");

        MsGraphGroup msGraphGroup = mock(MsGraphGroup.class);
        Config.KafkaConfig kafkaConfig = mock(Config.KafkaConfig.class);
        ConfigGroup configGroup = mock(ConfigGroup.class);
        ConcurrentHashMap<String, Optional<ResourceGroup>> resourceGroupCache = mock(ConcurrentHashMap.class);
        @SuppressWarnings("unchecked")
        Sinks.Many<Tuple2<String, Optional<ResourceGroup>>> resourceGroupSink = mock(Sinks.Many.class);
        when(resourceGroupCache.containsKey(eq(kafkaKeyID))).thenReturn(false);
        when(resourceGroupSink.asFlux()).thenReturn(Flux.never());
        when(resourceGroupSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        ResourceGroupConsumerService service = new ResourceGroupConsumerService(
                msGraphGroup,
                kafkaConfig,
                configGroup
        );
        ReflectionTestUtils.setField(service, "resourceGroupSink", resourceGroupSink);
        //service.setResourceGroupSink(resourceGroupSink); // critical for test injection
        ReflectionTestUtils.setField(service, "resourceGroupCache", resourceGroupCache);

        ConsumerRecord<String, ResourceGroup> record =
                new ConsumerRecord<>("test-topic", 0, 0L, kafkaKeyID, resourceGroup);

        // TODO: Fix after batching has been reimplemented
        // service.processEntityBatch(List.of(record));

        verify(resourceGroupCache, times(1)).put(eq(kafkaKeyID), any());
        verify(resourceGroupSink, times(1)).tryEmitNext(any());
    }

    // TODO: Fix after batching has been reimplemented and connections to SInk has been fixed
    @Disabled
    @Test
    void processLargeBatchOf2000NewRecords() {
        int batchSize = 2000;
        List<ConsumerRecord<String, ResourceGroup>> records = new ArrayList<>();

        MsGraphGroup msGraphGroup = mock(MsGraphGroup.class);

        Config.KafkaConfig kafkaConfig = mock(Config.KafkaConfig.class);
        ConfigGroup configGroup = mock(ConfigGroup.class);
        ConcurrentHashMap<String, Optional<ResourceGroup>> resourceGroupCache = mock(ConcurrentHashMap.class);
        @SuppressWarnings("unchecked")
        Sinks.Many<Tuple2<String, Optional<ResourceGroup>>> resourceGroupSink = mock(Sinks.Many.class);

        when(resourceGroupSink.asFlux()).thenReturn(Flux.never());
        when(resourceGroupSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        ResourceGroupConsumerService service = new ResourceGroupConsumerService(
                msGraphGroup,
                kafkaConfig,
                configGroup
        );

        // TODO: Reimplement after connections to SInk has been fixed
        // service.setResourceGroupSink(resourceGroupSink);

        ReflectionTestUtils.setField(service, "resourceGroupCache", resourceGroupCache);

        for (int i = 0; i < batchSize; i++) {
            String key = "key-" + i;
            ResourceGroup resourceGroup = newResourceGroupFromResourceName("Group-" + i);
            records.add(new ConsumerRecord<>("test-topic", 0, i, key, resourceGroup));

            when(resourceGroupCache.containsKey(eq(key))).thenReturn(false);
        }

        // TODO: Fix after batching has been reimplemented
        // service.processEntityBatch(records);

        verify(resourceGroupCache, times(batchSize)).put(anyString(), any());
        verify(resourceGroupSink, times(batchSize)).tryEmitNext(any());
    }

    // TODO: Fix after batching has been reimplemented
    @Disabled
    @Test
    void processEntityEntryAlreadyInCacheGeneratesNothing() {
        String kafkaKeyID = "TestKafkaKeyID";
        ResourceGroup resourceGroup = newResourceGroupFromResourceNameStatic();

        when(resourceGroupCache.containsKey(anyString())).thenReturn(true);
        when(resourceGroupCache.get(anyString())).thenReturn(Optional.ofNullable(resourceGroup));

        ConsumerRecord<String, ResourceGroup> record = new ConsumerRecord<>("topic", 0, 0L, kafkaKeyID, resourceGroup);

        // TODO: Fix after batching has been reimplemented
        // resourceGroupConsumerService.processEntityBatch(List.of(record));

        verify(resourceGroupCache, times(0)).put(anyString(), any());
        verify(resourceGroupSink, times(0)).tryEmitNext(any());
    }

    // TODO: Fix after batching has been reimplemented
    @Disabled
    @Test
    void processEntity_That_Is_Empty_And_Already_In_Cache_Generates_Nothing() {
        String kafkaKeyID = "TestKafkaKeyID";

        when(resourceGroupCache.containsKey(anyString())).thenReturn(true);
        when(resourceGroupCache.get(anyString())).thenReturn(Optional.empty());

        ConsumerRecord<String, ResourceGroup> record = new ConsumerRecord<>("topic", 0, 0L, kafkaKeyID, null);

        // TODO: Fix after batching has been reimplemented
        // resourceGroupConsumerService.processEntityBatch(List.of(record));

        verify(resourceGroupCache, times(0)).put(anyString(), any());
        verify(resourceGroupSink, times(0)).tryEmitNext(any());
    }

    // TODO: Fix after batching has been reimplemented
    @Disabled
    @Test
    void processEntity_That_Is_Empty_ResourceGroup_But_Not_In_Cache_Continues_Operation() {
        String kafkaKeyID = "TestKafkaKeyID";

        MsGraphGroup msGraphGroup = mock(MsGraphGroup.class);
        Config.KafkaConfig kafkaConfig = mock(Config.KafkaConfig.class);
        ConfigGroup configGroup = mock(ConfigGroup.class);
        ConcurrentHashMap<String, Optional<ResourceGroup>> resourceGroupCache = mock(ConcurrentHashMap.class);
        @SuppressWarnings("unchecked")
        Sinks.Many<Tuple2<String, Optional<ResourceGroup>>> resourceGroupSink = mock(Sinks.Many.class);

        when(resourceGroupSink.asFlux()).thenReturn(Flux.never());
        when(resourceGroupSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        when(resourceGroupCache.containsKey(eq(kafkaKeyID))).thenReturn(false);

        ResourceGroupConsumerService service = new ResourceGroupConsumerService(
                msGraphGroup,
                kafkaConfig,
                configGroup
        );
        // TODO: Reimplement after connections to SInk has been fixed
        // service.setResourceGroupSink(resourceGroupSink);
        ReflectionTestUtils.setField(service, "resourceGroupCache", resourceGroupCache);

        ConsumerRecord<String, ResourceGroup> record = new ConsumerRecord<>("topic", 0, 0L, kafkaKeyID, null);

        // TODO: Fix after batching has been reimplemented
        // service.processEntityBatch(List.of(record));

        verify(resourceGroupCache, times(1)).put(eq(kafkaKeyID), eq(Optional.empty()));
        verify(resourceGroupSink, times(1)).tryEmitNext(any());
    }

    // TODO: Reimplement after azure-update has been reimplemented
    @Disabled
    @Test
    void updateAzure_NewGroupCallsAzureCreate() throws Exception {

        String kafkaKeyID = "TestKafkaKeyID";

        when(msGraphGroup.doesGroupExist(anyString())).thenReturn(false);

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");
        // TODO: Reimplement after azure-update has been reimplemented
        // resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.ofNullable(resourceGroup));

        verify(msGraphGroup, times(1)).addGroupToAzureAsync(any());
        verify(msGraphGroup, times(0)).updateGroup(any());
        verify(msGraphGroup, times(0)).deleteGroupAsync(any());
    }
    // TODO: Reimplement after azure-update has been reimplemented

    @Test
    void updateAzure_UpdatedGroup_if_allowed() throws Exception {
        String kafkaKeyID = "TestKafkaKeyID";

        when(msGraphGroup.doesGroupExist(anyString())).thenReturn(true);
        when(configGroup.getAllowgroupupdate()).thenReturn(true);

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");
        // TODO: Reimplement after azure-update has been reimplemented
        // resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.ofNullable(resourceGroup));

        verify(msGraphGroup, times(0)).addGroupToAzureAsync(any());
        verify(msGraphGroup, times(1)).updateGroup(any());
        verify(msGraphGroup, times(0)).deleteGroupAsync(any());
    }

    // TODO: Reimplement after updateazure has been reimplemented
    @Disabled
    @Test
    void updateAzure_UpdatedGroup_if_not_allowed() throws Exception {
        String kafkaKeyID = "TestKafkaKeyID";

        when(msGraphGroup.doesGroupExist(anyString())).thenReturn(true);
        when(configGroup.getAllowgroupupdate()).thenReturn(false);

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");
        // TODO: Reimplement after updateazure has been reimplemented
        // resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.ofNullable(resourceGroup));

        verify(msGraphGroup, times(0)).addGroupToAzureAsync(any());
        verify(msGraphGroup, times(0)).updateGroup(any());
        verify(msGraphGroup, times(0)).deleteGroupAsync(any());
    }

    // TODO: Reimplement after updateazure has been reimplemented
    @Disabled
    @Test
    void updateAzure_DeletedGroup_If_Allowed_Calls_deleteGroup() throws Exception {
        String kafkaKeyID = "TestKafkaKeyID";

        when(configGroup.getAllowgroupdelete()).thenReturn(true);
        // TODO: Reimplement after updateazure has been reimplemented
        // resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.empty());

        verify(msGraphGroup, times(0)).addGroupToAzureAsync(any());
        verify(msGraphGroup, times(0)).updateGroup(any());
        verify(msGraphGroup, times(1)).deleteGroupAsync(any());
    }

    // TODO: Reimplement after updateazure has been reimplemented
    @Disabled
    @Test
    void updateAzure_DeletedGroup_If_Not_Allowed_Do_Not_Calls_deleteGroup() throws Exception {
        String kafkaKeyID = "TestKafkaKeyID";

        when(configGroup.getAllowgroupdelete()).thenReturn(false);
        // TODO: Reimplement after updateazure has been reimplemented
        // resourceGroupConsumerService.updateAzure(kafkaKeyID, Optional.empty());

        verify(msGraphGroup, times(0)).addGroupToAzureAsync(any());
        verify(msGraphGroup, times(0)).updateGroup(any());
        verify(msGraphGroup, times(0)).deleteGroupAsync(any());
    }
}
