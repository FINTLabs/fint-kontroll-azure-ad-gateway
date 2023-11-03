package no.fintlabs.kafka;

import com.microsoft.graph.core.BaseClient;
import com.microsoft.graph.core.IBaseClient;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.GraphServiceClient;
import jakarta.annotation.Resource;
import net.bytebuddy.utility.RandomString;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

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
    @InjectMocks
    private AzureClient azureClient;
    @InjectMocks
    private ResourceGroupConsumerService resourceGroupConsumerService;

    @Test
    void processEntity() {
        List<ResourceGroup> resourceGroups = new ArrayList<ResourceGroup>();
        when(azureClient.doesGroupExist(anyString())).thenReturn(false);
        //pre = afk
        //post = kon
        ResourceGroup group = new ResourceGroup(
                RandomStringUtils.random(4),
                "adc",
                "TestDisplayName " + RandomStringUtils.random(6),
                RandomStringUtils.random(12),
                "Adobe Cloud",
                "LicenseResource",
                "1000");

        resourceGroupConsumerService.processEntity(group, "testGroupID");
        //verify(resourceGroupConsumerService, times(1)).processEntity(any(ResourceGroup.class), anyString());

        ArgumentCaptor<Group> argcapGroup = ArgumentCaptor.forClass(Group.class);
        verify(graphServiceClient).groups().buildRequest().post(argcapGroup.capture());
        assertEquals("afk-lic-adobe.cloud-kon", argcapGroup.getValue().displayName);
//        ArgumentCaptor<Group> group = ArgumentCaptor.forClass(Group.class);
//        verify(azureClient).addGroupToAzure(group.capture());
//        assertEquals("afk-lic-adobe.cloud-kon", group.getValue().displayName());

        //afk-lic-adobe.cloud-kon

        /*ArgumentCaptor<Person> argument = ArgumentCaptor.forClass(Person.class);
        verify(mock).doSomething(argument.capture());
        assertEquals("John", argument.getValue().getName());*/

        // pre = bfk
        // post = kon
        resourceGroups.add(new ResourceGroup(
                RandomStringUtils.random(4),
                "ard",
                "TestDisplayName " + RandomStringUtils.random(6),
                RandomStringUtils.random(12),
                "ardoq",
                "ApplicationResource",
                "1000"));
        //bfk-app-ardoq-kon

        //pre = ofk
        //post = kon
        resourceGroups.add(new ResourceGroup(
                RandomStringUtils.random(4),
                "qlik",
                "TestDisplayName " + RandomStringUtils.random(6),
                RandomStringUtils.random(12),
                "qLikSense FK Inntak",
                "RoleResource",
                "1000"));
        //ofk-rol-qliksense.fk.inntak-kon*/

        //pre = afk
        //post = agg-kon
        resourceGroups.add(new ResourceGroup(
                RandomStringUtils.random(4),
                "adobek12",
                "TestDisplayName " + RandomStringUtils.random(6),
                RandomStringUtils.random(12),
                "Adobe K12 Utdanning",
                "ApplicationResource",
                "1000"));
        //afk-rol-adobe.k12.utdanning.agg-kon

        for (ResourceGroup resourceGroup : resourceGroups) {
            resourceGroupConsumerService.processEntity(resourceGroup, "testGroupID");
        }
        //<fylkeskode>-<ressurstype>-<ressursnavn små bokstaver, mellomrom erstattes med .>-agg-kon
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
