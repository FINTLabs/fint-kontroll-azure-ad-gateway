package no.fintlabs.kafka;

import net.bytebuddy.utility.RandomString;
import no.fintlabs.AzureClient;
import no.fintlabs.cache.FintCache;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import no.fintlabs.kafka.topic.EntityTopicService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
@ExtendWith(MockitoExtension.class)
class ResourceGroupMembershipConsumerServiceTest {

    @Mock
    private AzureClient azureClient;
    @Mock
    private FintCache<String, Optional> resourceGroupMembershipCache;
    @Mock
    private Sinks.Many<Tuple2<String, Optional<ResourceGroupMembership>>> resourceGroupMembershipSink;

    @InjectMocks
    private ResourceGroupMembershipConsumerService resourceGroupMembershipConsumerService;

    @Mock
    private EntityTopicService entityTopicService;

    static private ResourceGroupMembership exampleGroupMembership;
    static private String exampleKafkaKey;

    private List<ResourceGroupMembership> exampleGroupMemberships(int numberOfGroupMembers) {
        List<ResourceGroupMembership> groupMemberships = new ArrayList<>();
        for (int i = 0; i < numberOfGroupMembers; i++) {
            exampleGroupMembership = ResourceGroupMembership.builder()
                    .id("exampleID" + RandomString.make(3))
                    .azureUserRef("exampleUserRef" + RandomString.make(3))
                    .azureGroupRef("exampleGroupRef" + RandomString.make(3))
                    .roleRef("exampleRole")
                    .build();
            groupMemberships.add(exampleGroupMembership);
        }
    return groupMemberships;
    }

    private ResourceGroupMembership exampleGroupMembershipRandom() {
        return exampleGroupMembership = ResourceGroupMembership.builder()
                    .id("exampleID" + RandomString.make(3))
                    .azureUserRef("exampleUserRef" + RandomString.make(3))
                    .azureGroupRef("exampleGroupRef" + RandomString.make(3))
                    .roleRef("exampleRole")
                    .build();
    }

    @BeforeAll()
    static void setUpFirst() {
        exampleGroupMembership = ResourceGroupMembership.builder()
                .id("exampleID" + RandomString.make(3))
                .azureUserRef("exampleUserRef")
                .azureGroupRef("exampleGroupRef")
                .roleRef("exampleRole")
                .build();
        exampleKafkaKey = "testKey" + RandomString.make(3);
    }

//    @Test
//    void handleNonExistingKafkaQueue() {
//
//    }

    @Test
    void processEntityNewGroupmemberhipDetected() {

        resourceGroupMembershipConsumerService.processEntity(exampleGroupMembershipRandom(), "exampleID");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> verify(azureClient, times(1)).addGroupMembership(any(), anyString()));
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> verify(azureClient, times(0)).deleteGroupMembership(anyString()));
    }

    @Test
    void makeSureNullParametersDoesntCallAzureClient() {

        resourceGroupMembershipConsumerService.processEntity(null, null);

        verify(azureClient, times(0)).addGroupMembership(any(ResourceGroupMembership.class), anyString());
        verify(azureClient, times(0)).deleteGroupMembership(anyString());
    }

    @Test
    void makeSureCacheIsWrittenTo() {
        for (int i=0; i<10; i++) {
            resourceGroupMembershipConsumerService.processEntity(exampleGroupMembership, RandomStringUtils.randomAlphanumeric(6) + "_" + RandomStringUtils.randomAlphanumeric(6));
        }
        verify(resourceGroupMembershipCache, times(10)).put(anyString(), any(Optional.class));
    }
    @Test
    void makeSureProcessingNewKafkaidGetPutInCache() {
        String kafkaKey = "123";
        when(resourceGroupMembershipCache.containsKey(kafkaKey)).thenReturn(false);

        resourceGroupMembershipConsumerService.processEntity(exampleGroupMembership, kafkaKey);

        verify(resourceGroupMembershipCache, times(1)).put(anyString(), any(Optional.class));
    }

    @Test
    void makeSureProcessingSameKafkaidDoesntGetPutInCacheWhenMembershipIsSimilar() {
        String kafkaKey = "123";
        ResourceGroupMembership copyOfExampleMembership = exampleGroupMembership.toBuilder().build();

        when(resourceGroupMembershipCache.containsKey(kafkaKey)).thenReturn(true);
        when(resourceGroupMembershipCache.get(kafkaKey)).thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processEntity(copyOfExampleMembership, kafkaKey);

        verify(resourceGroupMembershipCache, times(0)).put(anyString(), any(Optional.class));
    }

    @Test
    void makeSureProcessingSameKafkaidGetsPutInCacheWhenMembershipIDIsChanged() {
        String kafkaKey = "123";
        ResourceGroupMembership copyOfExampleMembership = exampleGroupMembership.toBuilder()
                .id(exampleGroupMembership.getId() + "1")
                .build();

        when(resourceGroupMembershipCache.containsKey(kafkaKey)).thenReturn(true);
        when(resourceGroupMembershipCache.get(kafkaKey)).thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processEntity(copyOfExampleMembership, kafkaKey);

        verify(resourceGroupMembershipCache, times(1)).put(anyString(), any(Optional.class));
    }

    @Test
    void makeSureProcessingSameKafkaidGetsPutInCacheWhenMembershipAzureGroupRefIsChanged() {
        String kafkaKey = "123";
        ResourceGroupMembership copyOfExampleMembership = exampleGroupMembership.toBuilder()
                .azureGroupRef(exampleGroupMembership.getAzureGroupRef() + "1")
                .build();

        when(resourceGroupMembershipCache.containsKey(kafkaKey)).thenReturn(true);
        when(resourceGroupMembershipCache.get(kafkaKey)).thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processEntity(copyOfExampleMembership, kafkaKey);

        verify(resourceGroupMembershipCache, times(1)).put(anyString(), any(Optional.class));
    }

    @Test
    void makeSureProcessingSameKafkaidGetsPutInCacheWhenMembershipAzureUserRefIsChanged() {
        String kafkaKey = "123";
        ResourceGroupMembership copyOfExampleMembership = exampleGroupMembership.toBuilder()
                .azureUserRef(exampleGroupMembership.getAzureUserRef() + "1")
                .build();

        when(resourceGroupMembershipCache.containsKey(kafkaKey)).thenReturn(true);
        when(resourceGroupMembershipCache.get(kafkaKey)).thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processEntity(copyOfExampleMembership, kafkaKey);

        verify(resourceGroupMembershipCache, times(1)).put(anyString(), any(Optional.class));
    }

    @Test
    void makeSureProcessingSameKafkaidGetsPutInCacheWhenMembershipRoleRefIsChanged() {
        String kafkaKey = "123";
        ResourceGroupMembership copyOfExampleMembership = exampleGroupMembership.toBuilder()
                .roleRef(exampleGroupMembership.getRoleRef() + "1")
                .build();

        when(resourceGroupMembershipCache.containsKey(kafkaKey)).thenReturn(true);
        when(resourceGroupMembershipCache.get(kafkaKey)).thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processEntity(copyOfExampleMembership, kafkaKey);

        verify(resourceGroupMembershipCache, times(1)).put(anyString(), any(Optional.class));
    }


    @Test
    void makeSureObjectIsCreatedAndDeleted() {
        String kafkaKey1 = "1kafka_key";
        String kafkaKey2 = "2kafka_key";
        ResourceGroupMembership resourceGroupMembership1 = exampleGroupMembershipRandom();
        ResourceGroupMembership resourceGroupMembership2 = exampleGroupMembershipRandom();


        resourceGroupMembershipConsumerService.processEntity(resourceGroupMembership1, kafkaKey1);
        resourceGroupMembershipConsumerService.processEntity(null, kafkaKey1);
        resourceGroupMembershipConsumerService.processEntity(resourceGroupMembership2, kafkaKey2);
        resourceGroupMembershipConsumerService.processEntity(null, kafkaKey2);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> verify(azureClient, times(2)).addGroupMembership(any(ResourceGroupMembership.class), anyString()));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> verify(azureClient, times(2)).deleteGroupMembership(anyString()));
    }

    @Test
    void processEntityIsNewAndCacheIsUpdated() {
        resourceGroupMembershipConsumerService.setResourceGroupMembershipSink(this.resourceGroupMembershipSink);

        resourceGroupMembershipConsumerService.processEntity(exampleGroupMembership, exampleKafkaKey);

        verify(resourceGroupMembershipCache, times(1)).put(anyString(),any());
        verify(resourceGroupMembershipSink, times(1)).tryEmitNext(any());
    }
    @Test
    void processEntity_Membership_AlreadyInCacheGeneratesNothing() {
        resourceGroupMembershipConsumerService.setResourceGroupMembershipSink(this.resourceGroupMembershipSink);

        when(resourceGroupMembershipCache.containsKey(anyString())).thenReturn(true);
        when(resourceGroupMembershipCache.get(anyString())).thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processEntity(exampleGroupMembership, exampleKafkaKey);

        verify(resourceGroupMembershipCache, times(0)).put(anyString(),any());
        verify(resourceGroupMembershipSink, times(0)).tryEmitNext(any());
    }

    @Test
    void processEntity_Membership_SkipDeletionIfAlreadyDeleted() {
        resourceGroupMembershipConsumerService.setResourceGroupMembershipSink(this.resourceGroupMembershipSink);

        when(resourceGroupMembershipCache.containsKey(anyString())).thenReturn(true);
        when(resourceGroupMembershipCache.get(anyString())).thenReturn(Optional.empty());

        resourceGroupMembershipConsumerService.processEntity(null, exampleKafkaKey);

        verify(resourceGroupMembershipCache, times(0)).put(anyString(),any());
        verify(resourceGroupMembershipSink, times(0)).tryEmitNext(any());
    }

    @Test
    void updateAzureWithMembership_NewMembershipCallsAzureAddGroupMembership() {
        resourceGroupMembershipConsumerService.updateAzureWithMembership(exampleKafkaKey, Optional.of(exampleGroupMembership));

        verify(azureClient, times(1)).addGroupMembership(any(),anyString());
        verify(azureClient, times(0)).deleteGroupMembership(anyString());
    }

    @Test
    void updateAzureWithMembership_DeletedMembershipCallsAzureDeleteGroupMembership() {
        resourceGroupMembershipConsumerService.updateAzureWithMembership(exampleKafkaKey,Optional.empty());

        verify(azureClient, times(0)).addGroupMembership(any(),anyString());
        verify(azureClient, times(1)).deleteGroupMembership(anyString());
    }
}

