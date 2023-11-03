package no.fintlabs.kafka;

import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.GraphServiceClient;
import no.fintlabs.AzureClient;
import no.fintlabs.Config;
import no.fintlabs.ConfigGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

//@SpringBootTest
@ExtendWith(MockitoExtension.class)
//@RunWith(SpringRunner.class)
public class ResourceGroupConsumerServiceTest {

    @Mock
    private GraphServiceClient graphServiceClient;

/*    @MockBean
    private Config config;
    @MockBean
    private ConfigGroup configGroup;*/
    @Mock
    private ConfigGroup configGroup;
    //@Mock Group group;
    @Mock
    private Config config;

    @InjectMocks
    private AzureClient azureClient;
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
    void processEntityNewGroupGetsCallsAzureCreate() {

        when(azureClient.doesGroupExist(anyString())).thenReturn(false);

/*        List<ResourceGroup> resourceGroups = new ArrayList<ResourceGroup>();
        resourceGroups.add(newResourceGroupFromResourceName("Test app 3"));
        resourceGroups.add(newResourceGroupFromResourceName("Hmmmbopp"));*/

        ResourceGroup resourceGroup = newResourceGroupFromResourceName("Adobe Cloud");
        resourceGroupConsumerService.processEntity(resourceGroup, "testGroupID");

        //verify(resourceGroupConsumerService, times(1)).processEntity(any(ResourceGroup.class), anyString());

        /*ArgumentCaptor<Group> argcapGroup = ArgumentCaptor.forClass(Group.class);
        verify(graphServiceClient).groups().buildRequest().post(argcapGroup.capture());
        assertEquals("afk-lic-adobe.cloud-kon", argcapGroup.getValue().displayName);*/

        /*ArgumentCaptor<Person> argument = ArgumentCaptor.forClass(Person.class);
        verify(mock).doSomething(argument.capture());
        assertEquals("John", argument.getValue().getName());*/

        // TODO: Implement actual test
        /*when(graphServiceClient.groups()).thenReturn(
                new com.microsoft.graph.requests.GroupCollectionRequestBuilder(
                        "requestURL",
                        new BaseClient<>()
                )
        ) {
        }))*/
        //resourceGroupConsumerService = new ResourceGroupConsumerService(null, null,null, null);'
/*        String resourceGroupID = "testresourcegroup";
        ResourceGroup resourceGroup = new ResourceGroup(resourceGroupID, "testresource", "testDisplayname", "testidp", "testresourcename", "testresourcetype", "testresourcelimit");

        when(azureClient.doesGroupExist(resourceGroupID)).thenReturn(false);

        resourceGroupConsumerService.processEntity(resourceGroup, null);
        verify(azureClient, times(1));*/

        // asserts (hvis returnvalue)
        // verify (mockito)
        // Kan også verifisere input til kall. Sjekk parameterverdien er innafor <a,b> osv

    }
}
