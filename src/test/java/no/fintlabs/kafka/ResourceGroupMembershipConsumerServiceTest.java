package no.fintlabs.kafka;

import no.fintlabs.AzureClient;
import no.fintlabs.azure.AzureGroupMembershipProducerService;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@ExtendWith(MockitoExtension.class)
class ResourceGroupMembershipConsumerServiceTest {

    @Mock
    private AzureClient azureClient;
    @Mock
    private AzureGroupMembershipProducerService azureGroupMembershipProducerService;

    @Mock
    private ConcurrentHashMap<String, Optional<ResourceGroupMembership>> resourceGroupMembershipCache;
    @Mock
    private Sinks.Many<Tuple2<String, Optional<ResourceGroupMembership>>> resourceGroupMembershipSink;

    @InjectMocks
    private ResourceGroupMembershipConsumerService resourceGroupMembershipConsumerService;

    @Mock
    private EntityTopicService entityTopicService;

    static private ResourceGroupMembership exampleGroupMembership;
    static private String exampleKafkaKey;

    @BeforeAll()
    static void setUpFirst() {
        exampleGroupMembership = ResourceGroupMembership.builder()
                .id("exampleID")
                .azureUserRef("exampleUserRef")
                .azureGroupRef("exampleGroupRef")
                .roleRef("exampleRole")
                .build();
        exampleKafkaKey = "testKey";
    }

    @Test
    void handleNonExistingKafkaQueue() {

    }

//    @Test
//    void processEntityNewGroupmemberhipDetected() {
//
//        ResourceGroupMembership resourceGroupMembership = exampleGroupMembership.toBuilder()
//                .azureUserRef(null)
//                .build();
//
//        resourceGroupMembershipConsumerService.processEntity(resourceGroupMembership, "exampleID");
//
//        verify(azureClient, times(1)).addGroupMembership(any(ResourceGroupMembership.class), anyString());
//        verify(azureClient, times(0)).deleteGroupMembership(any(ResourceGroupMembership.class), anyString());
//    }

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

        verify(resourceGroupMembershipCache, times(0)).put(anyString(), any());
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


/*    @Test
    void makeSureObjectIsCreatedAndDeleted() {
        //List<Optional<ResourceGroupMembership>> resourceGroupMembershipList = new ArrayList<>();
        String kafkaKey = "SomeFakeID";
        ResourceGroupMembership rgmembership = null;
        resourceGroupMembershipConsumerService.processEntity(exampleGroupMembership, kafkaKey);
        resourceGroupMembershipConsumerService.processEntity(rgmembership, kafkaKey);
        resourceGroupMembershipConsumerService.processEntity(exampleGroupMembership, kafkaKey);
        resourceGroupMembershipConsumerService.processEntity(rgmembership, kafkaKey);

        verify(azureClient, times(2)).addGroupMembership(any(ResourceGroupMembership.class), anyString());
        verify(azureClient, times(2)).deleteGroupMembership(any(ResourceGroupMembership.class), anyString());
    }*/

    @Test
    void processEntityIsNewAndCacheIsUpdated() {
        resourceGroupMembershipConsumerService.setResourceGroupMembershipSink(this.resourceGroupMembershipSink);
        //when(resourceGroupMembershipSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        resourceGroupMembershipConsumerService.processEntity(exampleGroupMembership, exampleKafkaKey);

        verify(resourceGroupMembershipCache, times(1)).put(anyString(),any());
        verify(resourceGroupMembershipSink, times(1)).emitNext(any(),any());
    }
    @Test
    void processEntity_Membership_AlreadyInCacheStillProcesses() {
        resourceGroupMembershipConsumerService.setResourceGroupMembershipSink(this.resourceGroupMembershipSink);
        //when(resourceGroupMembershipSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        when(resourceGroupMembershipCache.containsKey(anyString())).thenReturn(true);
        when(resourceGroupMembershipCache.get(anyString())).thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processEntity(exampleGroupMembership, exampleKafkaKey);

        verify(resourceGroupMembershipCache, times(0)).put(anyString(), any());
        verify(resourceGroupMembershipSink, times(1)).emitNext(any(),any());
    }

    @Test
    void processEntity_Membership_DuplicateDeleteStillProcesses() {
        resourceGroupMembershipConsumerService.setResourceGroupMembershipSink(this.resourceGroupMembershipSink);
        //when(resourceGroupMembershipSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        when(resourceGroupMembershipCache.containsKey(anyString())).thenReturn(true);
        when(resourceGroupMembershipCache.get(anyString())).thenReturn(Optional.empty());

        resourceGroupMembershipConsumerService.processEntity(null, exampleKafkaKey);

        verify(resourceGroupMembershipCache, times(0)).put(anyString(), any());
        verify(resourceGroupMembershipSink, times(1)).emitNext(any(),any());
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

    @Test
    void emitNextRetriesOnOverflowAndEventuallyDelivers() throws Exception {
        Sinks.Many<Tuple2<String, Optional<String>>> sink =
                Sinks.many().multicast().onBackpressureBuffer(8);

        int total = 200;
        CountDownLatch latch = new CountDownLatch(total);
        AtomicInteger received = new AtomicInteger();

        sink.asFlux()
                .flatMap(t ->
                                Mono.delay(Duration.ofMillis(10))
                                        .doOnNext(ignored -> {
                                            received.incrementAndGet();
                                            latch.countDown();
                                        }),
                        1
                )
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        for (int i = 0; i < total; i++) {
            sink.emitNext(
                    Tuples.of("k" + i, Optional.of("v" + i)),
                    (st, er) -> er == Sinks.EmitResult.FAIL_OVERFLOW
                            || er == Sinks.EmitResult.FAIL_NON_SERIALIZED
            );
        }

        boolean allReceived = latch.await(5, TimeUnit.SECONDS);

        assertTrue(allReceived, "Did not receive all items. Received=" + received.get());
    }
}

