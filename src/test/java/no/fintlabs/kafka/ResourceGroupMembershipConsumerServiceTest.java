package no.fintlabs.kafka;

import net.bytebuddy.utility.RandomString;
import no.fintlabs.AzureClient;
import no.fintlabs.kafka.topic.name.EntityTopicNameParameters;
import no.fintlabs.kafka.topic.name.TopicNamePrefixParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import no.fintlabs.kafka.topic.EntityTopicService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Service
@ExtendWith(MockitoExtension.class)
class ResourceGroupMembershipConsumerServiceTest {

    @Mock
    private AzureClient azureClient;
    @Mock
    private ConcurrentHashMap<String, Optional<ResourceGroupMembership>> resourceGroupMembershipCache;
    @Mock
    private Sinks.Many<Tuple2<String, Optional<ResourceGroupMembership>>> resourceGroupMembershipSink;

    @InjectMocks
    private ResourceGroupMembershipConsumerService resourceGroupMembershipConsumerService;

    @Mock
    private EntityTopicService entityTopicService;

    @Mock
    private EntityTopicNameParameters entityTopicNameParameters;

    @Mock
    private TopicNamePrefixParameters topicNamePrefixParameters;

    static private ResourceGroupMembership exampleGroupMembership;
    static private String exampleKafkaKey;

    private ConsumerRecord<String, ResourceGroupMembership> rec(String key, ResourceGroupMembership value) {
        return new ConsumerRecord<>("resource-group-membership", 0, 0L, key, value);
    }

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

    private void wireMockSinkAndInit(
            ResourceGroupMembershipConsumerService service,
            Sinks.Many<Tuple2<String, Optional<ResourceGroupMembership>>> sink
    ) throws Exception {
        lenient().when(sink.asFlux()).thenReturn(Flux.never());
        lenient().when(sink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        Field f = ResourceGroupMembershipConsumerService.class
                .getDeclaredField("resourceGroupMembershipSink");
        f.setAccessible(true);
        f.set(service, sink);

        Method init = ResourceGroupMembershipConsumerService.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(service);
    }

    private void ensureInit(ResourceGroupMembershipConsumerService service) throws Exception {
        Method init = ResourceGroupMembershipConsumerService.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(service);
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
    void processEntityNewGroupmemberhipDetected() throws Exception {
        List<ResourceGroupMembership> members = exampleGroupMemberships(3);

        try {
            var init = ResourceGroupMembershipConsumerService.class.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(resourceGroupMembershipConsumerService);
        } catch (NoSuchMethodException ignored) { }

        List<ConsumerRecord<String, ResourceGroupMembership>> records = members.stream()
                .map(m -> new ConsumerRecord<>("resource-group-membership", 0, 0L, m.getId(), m))
                .toList();

        resourceGroupMembershipConsumerService.processMembershipBatch(records);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(azureClient, times(members.size()))
                        .addGroupMembership(any(ResourceGroupMembership.class), anyString()));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(azureClient, never()).deleteGroupMembership(anyString()));
    }



    @Test
    void makeSureNullParametersDoesntCallAzureClient() {
        resourceGroupMembershipConsumerService.processMembershipBatch(Collections.emptyList());

        verify(azureClient, times(0)).addGroupMembership(any(ResourceGroupMembership.class), anyString());
        verify(azureClient, times(0)).deleteGroupMembership(anyString());
    }

    @Test
    void makeSureCacheIsWrittenTo() {
        for (int i = 0; i < 10; i++) {
            String key = org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric(6) + "_" +
                    org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric(6);
            resourceGroupMembershipConsumerService.processMembershipBatch(
                    List.of(rec(key, exampleGroupMembership)));
        }
        verify(resourceGroupMembershipCache, times(10)).put(anyString(), any(Optional.class));
    }

    @Test
    void makeSureProcessingNewKafkaidGetPutInCache() {
        String kafkaKey = "123";
        when(resourceGroupMembershipCache.containsKey(kafkaKey)).thenReturn(false);

        resourceGroupMembershipConsumerService.processMembershipBatch(
                List.of(rec(kafkaKey, exampleGroupMembership)));

        verify(resourceGroupMembershipCache, times(1)).put(anyString(), any(Optional.class));
    }

    @Test
    void makeSureProcessingSameKafkaidDoesntGetPutInCacheWhenMembershipIsSimilar() {
        String kafkaKey = "123";
        ResourceGroupMembership copy = exampleGroupMembership.toBuilder().build();

        when(resourceGroupMembershipCache.containsKey(kafkaKey)).thenReturn(true);
        when(resourceGroupMembershipCache.get(kafkaKey)).thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processMembershipBatch(
                List.of(rec(kafkaKey, copy)));

        verify(resourceGroupMembershipCache, times(0)).put(anyString(), any(Optional.class));
    }

    @Test
    void makeSureProcessingSameKafkaidGetsPutInCacheWhenMembershipIDIsChanged() {
        String kafkaKey = "123";
        ResourceGroupMembership changed = exampleGroupMembership.toBuilder()
                .id(exampleGroupMembership.getId() + "1")
                .build();

        when(resourceGroupMembershipCache.containsKey(kafkaKey)).thenReturn(true);
        when(resourceGroupMembershipCache.get(kafkaKey)).thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processMembershipBatch(
                List.of(rec(kafkaKey, changed)));

        verify(resourceGroupMembershipCache, times(1)).put(anyString(), any(Optional.class));
    }

    @Test
    void makeSureProcessingSameKafkaidGetsPutInCacheWhenMembershipAzureGroupRefIsChanged() {
        String kafkaKey = "123";
        ResourceGroupMembership changed = exampleGroupMembership.toBuilder()
                .azureGroupRef(exampleGroupMembership.getAzureGroupRef() + "1")
                .build();

        when(resourceGroupMembershipCache.containsKey(kafkaKey)).thenReturn(true);
        when(resourceGroupMembershipCache.get(kafkaKey)).thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processMembershipBatch(
                List.of(rec(kafkaKey, changed)));

        verify(resourceGroupMembershipCache, times(1)).put(anyString(), any(Optional.class));
    }

    @Test
    void makeSureProcessingSameKafkaidGetsPutInCacheWhenMembershipAzureUserRefIsChanged() {
        String kafkaKey = "123";
        ResourceGroupMembership changed = exampleGroupMembership.toBuilder()
                .azureUserRef(exampleGroupMembership.getAzureUserRef() + "1")
                .build();

        when(resourceGroupMembershipCache.containsKey(kafkaKey)).thenReturn(true);
        when(resourceGroupMembershipCache.get(kafkaKey)).thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processMembershipBatch(
                List.of(rec(kafkaKey, changed)));

        verify(resourceGroupMembershipCache, times(1)).put(anyString(), any(Optional.class));
    }

    @Test
    void makeSureProcessingSameKafkaidGetsPutInCacheWhenMembershipRoleRefIsChanged() {
        String kafkaKey = "123";
        ResourceGroupMembership changed = exampleGroupMembership.toBuilder()
                .roleRef(exampleGroupMembership.getRoleRef() + "1")
                .build();

        when(resourceGroupMembershipCache.containsKey(kafkaKey)).thenReturn(true);
        when(resourceGroupMembershipCache.get(kafkaKey)).thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processMembershipBatch(
                List.of(rec(kafkaKey, changed)));

        verify(resourceGroupMembershipCache, times(1)).put(anyString(), any(Optional.class));
    }

    @Test
    void makeSureObjectIsCreatedAndDeleted() throws Exception {
        ensureInit(resourceGroupMembershipConsumerService);

        String key1 = "1kafka_key";
        String key2 = "2kafka_key";

        ResourceGroupMembership m1 = exampleGroupMembershipRandom()
                .toBuilder().azureUserRef("user-1").azureGroupRef("group-1").build();
        ResourceGroupMembership m2 = exampleGroupMembershipRandom()
                .toBuilder().azureUserRef("user-2").azureGroupRef("group-2").build();

        // Sequence: add m1, delete key1, add m2, delete key2
        resourceGroupMembershipConsumerService.processMembershipBatch(List.of(rec(key1, m1)));
        resourceGroupMembershipConsumerService.processMembershipBatch(List.of(rec(key1, null)));
        resourceGroupMembershipConsumerService.processMembershipBatch(List.of(rec(key2, m2)));
        resourceGroupMembershipConsumerService.processMembershipBatch(List.of(rec(key2, null)));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(azureClient, times(2))
                        .addGroupMembership(any(ResourceGroupMembership.class), anyString()));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(azureClient, times(2))
                        .deleteGroupMembership(anyString()));
    }

    @Test
    void processEntityIsNewAndCacheIsUpdated() throws Exception {
        wireMockSinkAndInit(resourceGroupMembershipConsumerService, this.resourceGroupMembershipSink);

        resourceGroupMembershipConsumerService.processMembershipBatch(
                List.of(rec(exampleKafkaKey, exampleGroupMembership)));

        verify(resourceGroupMembershipCache, times(1)).put(anyString(), any());
        verify(resourceGroupMembershipSink, times(1)).tryEmitNext(any());
    }

    @Test
    void processEntity_Membership_AlreadyInCacheGeneratesNothing() throws Exception {
        wireMockSinkAndInit(resourceGroupMembershipConsumerService, this.resourceGroupMembershipSink);

        lenient().when(resourceGroupMembershipCache.containsKey(anyString())).thenReturn(true);
        lenient().when(resourceGroupMembershipCache.get(anyString()))
                .thenReturn(Optional.of(exampleGroupMembership));

        resourceGroupMembershipConsumerService.processMembershipBatch(
                List.of(rec(exampleKafkaKey, exampleGroupMembership)));

        verify(resourceGroupMembershipCache, times(0)).put(anyString(), any());
        verify(resourceGroupMembershipSink, times(0)).tryEmitNext(any());
    }

    @Test
    void processEntity_Membership_SkipDeletionIfAlreadyDeleted() throws Exception {
        wireMockSinkAndInit(resourceGroupMembershipConsumerService, this.resourceGroupMembershipSink);

        lenient().when(resourceGroupMembershipCache.containsKey(anyString())).thenReturn(true);
        lenient().when(resourceGroupMembershipCache.get(anyString()))
                .thenReturn(Optional.empty());

        resourceGroupMembershipConsumerService.processMembershipBatch(
                List.of(rec(exampleKafkaKey, null)));

        verify(resourceGroupMembershipCache, times(0)).put(anyString(), any());
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

