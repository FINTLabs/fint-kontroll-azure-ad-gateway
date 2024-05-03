package no.fintlabs.kafka;

import no.fintlabs.EntraClient;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;

@Service
@ExtendWith(MockitoExtension.class)
class ResourceGroupMembershipConsumerServiceTest {

    @Mock
    private EntraClient entraClient;
    @Mock
    private FintCache<String, Optional> resourceGroupMembershipCache;

    @InjectMocks
    private ResourceGroupMembershipConsumerService resourceGroupMembershipConsumerService;

    @Mock
    private EntityTopicService entityTopicService;

    static private ResourceGroupMembership exampleGroupMembership;

    @BeforeAll()
    static void setUpFirst() {
        exampleGroupMembership = ResourceGroupMembership.builder()
                .id("exampleID")
                .azureUserRef("exampleUserRef")
                .azureGroupRef("exampleGroupRef")
                .roleRef("exampleRole")
                .build();
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
    void makeSureWhenKeyExistButMembershipIsAlmostEmpty() {

        String rGroupKey = "exampleID";
        resourceGroupMembershipConsumerService.processEntity(ResourceGroupMembership.builder().azureGroupRef("123").azureUserRef("234").build(), rGroupKey);

        verify(entraClient, times(1)).addGroupMembership(any(ResourceGroupMembership.class), anyString());
        verify(entraClient, times(0)).deleteGroupMembership(null, rGroupKey);
    }

    @Test
    void makeSureNullGroupMembershipIsDeletedWhenKeyIsDefined() {

        String rGroupKey = "exampleID";
        resourceGroupMembershipConsumerService.processEntity(null, rGroupKey);

        verify(resourceGroupMembershipCache, times(1)).put(rGroupKey, Optional.empty());

        verify(entraClient, times(0)).addGroupMembership(any(ResourceGroupMembership.class), anyString());
        verify(entraClient, times(1)).deleteGroupMembership(null, rGroupKey);
    }

    @Test
    void makeSureNullParametersDoesntCallAzureClient() {

        resourceGroupMembershipConsumerService.processEntity(null, null);

        verify(entraClient, times(0)).addGroupMembership(any(ResourceGroupMembership.class), anyString());
        verify(entraClient, times(0)).deleteGroupMembership(any(ResourceGroupMembership.class), anyString());
    }

    @Test
    void processEntityDeleteGroupmemberhip() {
        resourceGroupMembershipConsumerService.processEntity(null, "exampleID");

        verify(entraClient, times(0)).addGroupMembership(any(ResourceGroupMembership.class), anyString());
        verify(entraClient, times(1)).deleteGroupMembership(null, "exampleID");
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
}