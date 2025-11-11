package no.fintlabs.group;

import com.microsoft.graph.core.tasks.PageIterator;
import com.microsoft.graph.groups.delta.DeltaGetResponse;
import com.microsoft.graph.groups.delta.DeltaRequestBuilder;
import com.microsoft.graph.models.DirectoryObjectCollectionResponse;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.GroupCollectionResponse;
import com.microsoft.graph.models.ReferenceCreate;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.serialization.UntypedArray;
import com.microsoft.kiota.serialization.UntypedObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azure.*;
import no.fintlabs.config.Config;
import no.fintlabs.config.ConfigGroup;
import no.fintlabs.core.*;
import no.fintlabs.core.entity.CoreMembership;
import no.fintlabs.kafka.ResourceGroup;
import no.fintlabs.kafka.ResourceGroupMembership;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Component
@Slf4j
@RequiredArgsConstructor
public class MsGraphGroup {
    protected final Config config;
    protected final ConfigGroup configGroup;
    protected final GraphServiceClient graphServiceClient;
    //private final ConcurrentHashMap<String, Optional<ResourceGroupMembership>> resourceGroupMembershipCache;
    //private final Set<String> membershipCache = ConcurrentHashMap.newKeySet();
    //private final ConcurrentHashMap<String, AzureGroup> azureGroupCache;
    //DBObjectList<DBGroup> azureGroupCache;
    //DBObjectList<AzureGroup> resourceGroupMembershipCache;
    //DBObjectList<DBMembership> membershipCache;
    private final CoreObjectListOrchestrator orchestrator;
    private final AzureGroupProducerService azureGroupProducerService;
    private final AzureGroupMembershipProducerService azureGroupMembershipProducerService;
    private final ExecutorService groupExecutor = Executors.newFixedThreadPool(10);
    private boolean fullImport = false;
    private String odataGroupDeltaLink;
    private AtomicInteger numMembers = new AtomicInteger(0);
    private final AtomicInteger addedMemberships = new AtomicInteger(0);
    private final AtomicInteger removedMemberships = new AtomicInteger(0);
    private AtomicInteger groupCounter;
    private Set<String> processedGroupIds;

    @Value("${fint.kontroll.azure-ad-gateway.group-scheduler.delta-pull.quickstart}")
    private boolean quickDeltaStart;

    @Scheduled(cron = "${fint.kontroll.azure-ad-gateway.group-scheduler.clear-cache}")
    public void clearCaches() {
        fullImport = true;
        orchestrator.clear();
        log.info("Delta caches for group has been reset to null due to scheduler. Next call will fetch all groups from Entra ID using delta call");
    }

    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.delta-pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.group-scheduler.delta-pull.delta-delay-ms}"
    )
    public void pullAllGroupsDelta() {
        log.info("*** <<< Fetching groups and members using delta call from Microsoft Graph >>> ***");
        long startMs = System.currentTimeMillis();
        numMembers.set(0);
        processedGroupIds = ConcurrentHashMap.newKeySet();
        addedMemberships.set(0);
        removedMemberships.set(0);

        if (fullImport) odataGroupDeltaLink = null;
        if (!fullImport && quickDeltaStart && orchestrator.getGroups().isEmpty()) {
            odataGroupDeltaLink = graphServiceClient
                    .getRequestAdapter()
                    .getBaseUrl()
                    + "/groups/delta?$deltatoken=latest&$select="
                    + String.join(",", configGroup.getAllGroupAttributes());
            quickDeltaStart = false;
        }


        try {
            Consumer<DeltaRequestBuilder.GetRequestConfiguration> initialCfg = requestConfiguration -> {
                requestConfiguration.queryParameters.select = configGroup.getAllGroupAttributes();
                requestConfiguration.queryParameters.top = configGroup.getGrouppagingsize();
            };

            DeltaGetResponse current =
                    (odataGroupDeltaLink != null && !odataGroupDeltaLink.isBlank())
                            ? graphServiceClient.groups().delta().withUrl(odataGroupDeltaLink).get()
                            : graphServiceClient.groups().delta().get(initialCfg);

            DeltaGetResponse lastPage = current;
            CompletableFuture<DeltaGetResponse> nextFuture = null;
            final String attr = configGroup.getFintkontrollidattribute();

            while (current != null) {
                if (current.getValue() != null) {
                    for (Group g : current.getValue()) {
                        String name = g.getDisplayName();
                        Map<String, Object> ad = g.getAdditionalData();
                        if (name == null || !name.endsWith(configGroup.getSuffix()) || ad == null || !ad.containsKey(attr)) {
                            continue;
                        }
                        processSingleGroup(g);
                    }
                }

                String next = current.getOdataNextLink();
                if (next == null) break;

                if (nextFuture == null) {
                    nextFuture = CompletableFuture.supplyAsync(
                            () -> graphServiceClient.groups().delta().withUrl(next).get(), groupExecutor);
                    current = nextFuture.join();
                    lastPage = current;
                } else {
                    current = nextFuture.join();
                    lastPage = current;
                    String following = current.getOdataNextLink();
                    nextFuture = (following == null) ? null
                            : CompletableFuture.supplyAsync(
                            () -> graphServiceClient.groups().delta().withUrl(following).get(), groupExecutor);
                    if (nextFuture == null) break;
                }
                log.info("Processed memberships so far: {}", addedMemberships.get());
            }

            String newDelta = (lastPage != null) ? lastPage.getOdataDeltaLink() : null;
            if (newDelta == null || newDelta.isBlank()) {
                log.warn("Last page doesn't contain @odata.deltaLink; keeping previous token.");
                return;
            } else {
                if (odataGroupDeltaLink == null) {
                    log.info("*** <<< Initial Delta run on Groups completed >>> ***");
                }
                odataGroupDeltaLink = newDelta;
                log.debug("*** <<< odataGroupDeltaLink updated. Finished pullAllGroupsDelta >>> ***");
            }

        } catch (ApiException e) {
            log.error("ApiException when trying to get groups using delta: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected exception when processing groups: {}", e.getMessage());
        }
        finally {
            fullImport = false;
        }

        long elapsedSec = (System.currentTimeMillis() - startMs) / 1000;
        long minutes = elapsedSec / 60, seconds = elapsedSec % 60;
        int adds = addedMemberships.get();
        int removes = removedMemberships.get();

        if (!processedGroupIds.isEmpty() || adds > 0 || removes > 0) {
            log.info("*** <<< Found {} groups with suffix \"{}\"; memberships changed ({} added / {} removed). "
                            + "Published to Kafka in {} minutes and {} seconds >>> ***",
                    processedGroupIds.size(),
                    configGroup.getSuffix(),
                    adds,
                    removes,
                    minutes,
                    seconds
            );
        } else {
            log.info("*** <<< No membership changes since last delta call on groups from Graph. "
                            + "Finished in {} minutes and {} seconds >>> ***",
                    minutes,
                    seconds
            );
        }
    }

    private void processSingleGroup(Group group) {
        try {
            String groupId = group.getId();

            if (processedGroupIds.add(groupId)) {
                AzureGroup newGroup = new AzureGroup(group, configGroup);
                azureGroupProducerService.processGroup(newGroup);
            }
            processMembersDelta(group);

        } catch (Exception e) {
            log.error("Error processing group {}. Skipping.", group.getId(), e);
        }
    }

    private void processMembersDelta(Group group) {
        Map<String, Object> additionalData = group.getAdditionalData();
        if (!additionalData.containsKey("members@delta")) {
            return;
        }
        try {
            Object membersDeltaObject = additionalData.get("members@delta");
            UntypedArray membersDeltaArray = (UntypedArray) membersDeltaObject;

            for (Object member : membersDeltaArray.getValue()) {
                UntypedObject untypedMember = (UntypedObject) member;
                String memberType = (String) untypedMember.getValue().get("@odata.type").getValue();
                String memberId = (String) untypedMember.getValue().get("id").getValue();

                if (!"#microsoft.graph.user".equals(memberType)) {
                    continue;
                }

                HashKey key;
                try {
                    key = CoreMembershipMapper.toCoreMembershipHashKey(UUID.fromString(memberId), Long.getLong(group.getId()));
                } catch (Exception e) {
                    log.error(e.getMessage());
                    continue;
                }

                if (untypedMember.getValue().containsKey("@removed")) {
                    if (orchestrator.getMemberships().containsKey(key)) {
                        orchestrator.getMemberships().remove(key);
                        azureGroupMembershipProducerService.removeMembership(
                                new AzureGroupMembership(memberId, group.getId(), key.toString())
                        );
                        removedMemberships.incrementAndGet();
                        log.debug("User {} is no longer member of group {}", memberId, group.getId());
                    } else {
                        log.debug("Remove event for {}, but cache had no membership (already removed).", key);
                    }
                    continue;
                }

                // TODO: This should never happen. ID is updated with new object.
                CoreMembership oldval = orchestrator.getMemberships().putIfAbsent(key, CoreMembershipMapper.toCoreMembership(UUID.fromString(memberId), Long.getLong(group.getId()), orchestrator.getUsers(), orchestrator.getGroups()));
                if (oldval == null) {
                    AzureGroupMembership m = new AzureGroupMembership(memberId, group.getId(), key.toString());
                    azureGroupMembershipProducerService.addMembership(m);
                    addedMemberships.incrementAndGet();
                    log.debug("User {} is member of group {}", memberId, group.getId());
                } else {
                    log.debug("Add/update for {}, but membership already present (duplicate).", key);
                }
            }

        } catch (ClassCastException e) {
            log.error("Failed to process members@delta. Error: {}", e.getMessage());
        }
    }

    public void pullAllGroupsAsync() {
        log.info("*** <<< Fetching groups from Microsoft Graph >>> ***");
        long startTime = System.currentTimeMillis();
        numMembers = new AtomicInteger(0);
        groupCounter = new AtomicInteger(0);

        CompletableFuture.supplyAsync(() -> {
            try {
                return pageThroughGroups(graphServiceClient.groups()
                        .get(requestConfiguration -> {
                            requestConfiguration.queryParameters.select = configGroup.getAllGroupAttributes();
                            requestConfiguration.queryParameters.top = configGroup.getGrouppagingsize();
                        }));
            } catch (ApiException | ReflectiveOperationException e) {
                log.error("Failed when trying to get groups. ", e);
                return new ArrayList<AzureGroup>();
            }
        }, groupExecutor).thenAccept(allGroups -> {
            long endTime = System.currentTimeMillis();
            long elapsedTimeInSeconds = (endTime - startTime) / 1000;
            long minutes = elapsedTimeInSeconds / 60;
            long seconds = elapsedTimeInSeconds % 60;
            log.info("*** <<< {} groups with suffix \"{}\" processed in {} minutes and {} seconds. Continuing processing memberships >>> ***", groupCounter, configGroup.getSuffix(), minutes, seconds);
            fetchAndPublishMembersForAllGroupsAsync(allGroups);

        }).exceptionally(ex -> {
            log.error("An error occurred while fetching groups: {}", ex.getMessage());
            return null;
        }).thenRun(() -> {
            long endTime = System.currentTimeMillis();
            long elapsedTimeInSeconds = (endTime - startTime) / 1000;
            long minutes = elapsedTimeInSeconds / 60;
            long seconds = elapsedTimeInSeconds % 60;
            log.info("*** <<< Done processing memberships in {} minutes and {} seconds >>> ***", minutes, seconds);

            if (numMembers.get() > 0) {
                log.info("*** <<< {} Entra group memberships published to kafka as they were not in membership cache >>> ***", numMembers.get());
            } else {
                log.info("*** <<< All entra group members already in cache. No members published to kafka >>> ***");
            }
        }).join();
    }

    private List<AzureGroup> pageThroughGroups(GroupCollectionResponse groupPage) throws ReflectiveOperationException {
        List<AzureGroup> allGroups = new ArrayList<>();

        PageIterator<Group, GroupCollectionResponse> pageIterator = new PageIterator.Builder<Group, GroupCollectionResponse>()
                .client(graphServiceClient)
                .collectionPage(groupPage)
                .collectionPageFactory(GroupCollectionResponse::createFromDiscriminatorValue)
                .processPageItemCallback(group -> {
                    boolean shouldProcess = group.getDisplayName() != null
                            && group.getDisplayName().endsWith(configGroup.getSuffix())
                            && group.getAdditionalData() != null
                            && group.getAdditionalData().containsKey(configGroup.getFintkontrollidattribute());

                    if (!shouldProcess) return true;

                    AzureGroup newGroup = new AzureGroup(group, configGroup);
                    if (orchestrator.getGroups() != null
                            && orchestrator.getGroups().containsKey(Long.getLong(newGroup.getId()))
                            && newGroup.equals(orchestrator.getGroups().get(Long.getLong(newGroup.getId())))) {
                        log.info("{} groupID already published and in cache. Not republished to kafka", newGroup.getId());
                    } else {
                        groupCounter.incrementAndGet();
                        azureGroupProducerService.processGroup(newGroup);
                        orchestrator.getGroups().put(Long.getLong(newGroup.getId()), CoreGroupMapper.toCoreGroup(newGroup));
                        allGroups.add(newGroup);
                    }
                    return true;
                }).build();

        pageIterator.iterate();
        return allGroups;
    }

    public void fetchAndPublishMembersForAllGroupsAsync(List<AzureGroup> groups) {
        List<CompletableFuture<Void>> memberFutures = groups.stream()
                .map(group -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return graphServiceClient.groups()
                                .byGroupId(group.getId())
                                .members()
                                .get(requestConfiguration -> {
                                    requestConfiguration.queryParameters.select = new String[]{"id"};
                                    requestConfiguration.queryParameters.top = configGroup.getGrouppagingsize();
                                });
                    } catch (Exception e) {
                        log.error("Error fetching members for group {}: {}", group.getId(), e.getMessage());
                        return null;
                    }
                }, groupExecutor).thenCompose(memberPage -> {
                    if (memberPage != null) {
                        return pageThroughAzureGroupAsync(group, memberPage);
                    }
                    return CompletableFuture.completedFuture(null);
                }))
                .toList();

        CompletableFuture.allOf(memberFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("*** <<< Finished fetching members for all groups >>> ***"))
                .join();
    }

    private CompletableFuture<Void> pageThroughAzureGroupAsync(AzureGroup azureGroup,
                                                               DirectoryObjectCollectionResponse inPage) {
        AtomicInteger membersPerGroupCount = new AtomicInteger(0);

        return processPageAsync(azureGroup, inPage, membersPerGroupCount)
                .thenRun(() -> log.debug("{} memberships detected in groupName \"{}\" with groupId {}",
                        membersPerGroupCount.get(), azureGroup.getDisplayName(), azureGroup.getId()));
    }

    private CompletableFuture<Void> processPageAsync(AzureGroup azureGroup,
                                                     DirectoryObjectCollectionResponse page, AtomicInteger membersCount) {
        if (page == null) {
            return CompletableFuture.completedFuture(null);
        }

        page.getValue().forEach(member -> {
            AzureGroupMembership azureGroupMembership = new AzureGroupMembership(azureGroup.getId(), member);
            if (orchestrator.getMemberships() != null
                    && orchestrator.getMemberships().containsKey(CoreMembershipMapper.toCoreMembershipHashKey(azureGroupMembership))) {
                log.debug("Skipping message to Kafka, as userId: {} is already published as member of groupId: {}", member.getId(), azureGroup.getId());
            } else {
                azureGroupMembershipProducerService.publishAddedMembership(azureGroupMembership);
                orchestrator.getMemberships().put(CoreMembershipMapper.toCoreMembershipHashKey(azureGroupMembership), CoreMembershipMapper.toCoreMembership(azureGroupMembership, orchestrator.getUsers(), orchestrator.getGroups()));
                membersCount.getAndIncrement();
                numMembers.getAndIncrement();
                log.debug("Produced message to Kafka where userId: {} is member of groupId: {}", member.getId(), azureGroup.getId());
            }

        });

        if (page.getOdataNextLink() != null) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return graphServiceClient.groups()
                            .byGroupId(azureGroup.getId())
                            .members()
                            .withUrl(page.getOdataNextLink())
                            .get();
                } catch (Exception e) {
                    log.error("Error fetching next member page for group {}: {}", azureGroup.getId(), e.getMessage());
                    return null;
                }
            }, groupExecutor).thenCompose(nextPage -> {
                if (nextPage != null) {
                    return processPageAsync(azureGroup, nextPage, membersCount);
                }
                return CompletableFuture.completedFuture(null);
            });
        }

        return CompletableFuture.completedFuture(null);
    }

    public boolean doesGroupExist(String resourceGroupId) throws Exception {
        String filterCriteria = String.format(configGroup.getFintkontrollidattribute() + " eq '%s'", resourceGroupId);

        GroupCollectionResponse groupCollectionPage = graphServiceClient.groups()
                .get(requestConfiguration -> {
                    requestConfiguration.queryParameters.select = configGroup.getGroupAttributesNotMembers();
                    requestConfiguration.queryParameters.filter = filterCriteria;
                });

        if (groupCollectionPage.getOdataNextLink() != null) {
            throw new Exception("doesGroupExist should only return a single group!");
        }

        for (Group group : groupCollectionPage.getValue()) {
            String attributeValue = group.getAdditionalData().get(configGroup.getFintkontrollidattribute()).toString();

            if (attributeValue != null && attributeValue.equals(resourceGroupId)) {
                return true;
            }
        }

        return false;
    }

    public void addGroupToAzureAsync(ResourceGroup resourceGroup) {
        if (resourceGroup.getResourceName() != null &&
                !resourceGroup.getResourceName().trim().isEmpty() &&
                resourceGroup.getResourceType() != null &&
                !resourceGroup.getResourceType().trim().isEmpty() &&
                resourceGroup.getResourceType().length() > 3) {

            Group group = new MsGraphGroupMapper().toMsGraphGroup(resourceGroup, configGroup, config);
            String owner = "https://graph.microsoft.com/v1.0/directoryObjects/" + config.getCredentials().getEntobjectid();
            HashMap<String, Object> additionalData = new HashMap<>();
            LinkedList<String> ownersOdataBind = new LinkedList<>();
            ownersOdataBind.add(owner);
            additionalData.put("owners@odata.bind", ownersOdataBind);
            additionalData.put(configGroup.getFintkontrollidattribute(), resourceGroup.getId());
            group.setAdditionalData(additionalData);

            //TODO: Consider if uniqueName should be set upon creation of group
            //group.setUniqueName(resourceGroup.getId());

            CompletableFuture.runAsync(() -> {
                try {
                    Group createdGroup = graphServiceClient
                            .groups()
                            .post(group);

                    if (createdGroup != null) {
                        log.info("Added Group to Azure: {}", createdGroup.getDisplayName());
                        azureGroupProducerService.processGroup(new AzureGroup(createdGroup, configGroup));
                    }
                } catch (ApiException e) {
                    log.warn(e.getMessage());
                }

            }, groupExecutor).exceptionally(ex -> {
                log.error("Exception while adding group: {}", ex.getMessage(), ex);
                return null;
            });
        } else {
            log.error("addGroupToAzure cannot be completed as ResourceGroup with ID: {} does not have all required attributes set", resourceGroup.getId());
        }
    }

    public void deleteGroupAsync(String resourceGroupId) {
        if (resourceGroupId == null || resourceGroupId.trim().isEmpty()) {
            log.error("deleteGroup cannot be completed: resourceGroupId is null/blank");
            return;
        }

        final String attr = configGroup.getFintkontrollidattribute();

        CompletableFuture.runAsync(() -> {
            try {
                GroupCollectionResponse page = graphServiceClient
                        .groups()
                        .get(requestConfiguration -> {
                            requestConfiguration.queryParameters.select = new String[]{"id," + attr};
                            requestConfiguration.queryParameters.filter = attr + " eq '" + resourceGroupId + "'";
                            requestConfiguration.queryParameters.top = 2;
                        });

                if (page == null || page.getValue() == null || page.getValue().isEmpty()) {
                    log.warn("No group found for {}={} (nothing to delete)", attr, resourceGroupId);
                    return;
                }

                var matches = page.getValue().stream()
                        .filter(g -> resourceGroupId.equals(g.getAdditionalData().get(attr)))
                        .toList();

                if (matches.isEmpty()) {
                    log.warn("No group matched {}={} after filtering (nothing to delete)", attr, resourceGroupId);
                    return;
                }
                if (matches.size() > 1) {
                    log.error("Expected exactly 1 group, found {} for {}={}. Aborting delete.", matches.size(), attr, resourceGroupId);
                    return;
                }

                String groupId = matches.getFirst().getId();

                try {
                    graphServiceClient
                            .groups()
                            .byGroupId(groupId)
                            .delete();

                    log.info("Group objectId {} and {}={} deleted", groupId, attr, resourceGroupId);
                } catch (com.microsoft.kiota.ApiException e) {
                    log.error("Failed to delete group {} ({}={}). Error: {}", groupId, attr, resourceGroupId, e.getMessage());
                }
            } catch (com.microsoft.kiota.ApiException e) {
                log.error("Failed to query groups for {}={}. Error: {}", attr, resourceGroupId, e.getMessage());
            }
        }, groupExecutor).exceptionally(e -> {
            log.error("Exception while scheduling deleteGroup for {}={}. Error: {}", attr, resourceGroupId, e.getMessage());
            return null;
        });
    }

    public void deleteGroup(String resourceGroupId) {
        GroupCollectionResponse groupCollectionPage = null;
        String[] selectionCriteria = new String[]{String.format("id,%s", configGroup.getFintkontrollidattribute())};
        String filterCriteria = String.format(configGroup.getFintkontrollidattribute() + " eq '%s'", resourceGroupId);
        try {
            groupCollectionPage = graphServiceClient.groups()
                    .get(requestConfiguration -> {
                        requestConfiguration.queryParameters.select = selectionCriteria;
                        requestConfiguration.queryParameters.filter = filterCriteria;
                    });
        } catch (ApiException e) {
            log.error("Failed find the group in graph to be deleted using resourceGroupId {}: {}", resourceGroupId, e.getMessage());
        }

        while (groupCollectionPage != null) {
            for (Group group : groupCollectionPage.getValue()) {
                Object attributeValue = group.getAdditionalData().get(configGroup.getFintkontrollidattribute());

                if (attributeValue != null && attributeValue.equals(resourceGroupId)) {
                    try {
                        graphServiceClient.groups()
                                .byGroupId(group.getId())
                                .delete();
                        log.info("Group objectId {} and resourceGroupId {} deleted ", group.getId(), resourceGroupId);
                        return;
                    } catch (ApiException e) {
                        log.error("Failed to delete group with objectId: {} and resourceGroupId: {} \n Error message: {}", group.getId(), resourceGroupId, e.getMessage());
                        throw e;
                    }
                }
            }
        }
    }

    public void updateGroup(ResourceGroup resourceGroup) {
        Group group = new MsGraphGroupMapper().toMsGraphGroup(resourceGroup, configGroup, config);
        //group.setOwners(null);
        //group.setAdditionalData(null);

        //LinkedList<Option> requestOptions = new LinkedList<>();
        //requestOptions.add(new HeaderOption("Prefer", "create-if-missing"));
        String identityProviderGroupObjectId = resourceGroup.getIdentityProviderGroupObjectId();
        CompletableFuture.runAsync(() -> {
            try {
                Group groupResponse = graphServiceClient.groups()
                        .byGroupId(identityProviderGroupObjectId)
                        .patch(group);
                if (groupResponse != null) {
                    log.info("Group with GroupObjectId '{}' successfully updated", identityProviderGroupObjectId);
                }
            } catch (ApiException e) {
                log.error("Failed to update group with GroupObjectId '{}': {}", identityProviderGroupObjectId, e.getMessage());
            }
        }, groupExecutor).exceptionally(e -> {
            log.error("Exception while update Group for {}. Error: {}", identityProviderGroupObjectId, e.getMessage());
            return null;
        });
    }

    public void addGroupMembership(ResourceGroupMembership resourceGroupMembership, String resourceGroupMembershipKey) {
        if (resourceGroupMembership.getAzureUserRef() != null && resourceGroupMembership.getAzureGroupRef() != null) {
            com.microsoft.graph.models.DirectoryObject directoryObject = new com.microsoft.graph.models.DirectoryObject();
            directoryObject.setId(resourceGroupMembership.getAzureUserRef());
            ReferenceCreate referenceMember = new ReferenceCreate();
            referenceMember.setOdataId(String.format("https://graph.microsoft.com/v1.0/directoryObjects/%s", resourceGroupMembership.getAzureUserRef()));
            CompletableFuture.runAsync(() -> {
                try {
                    graphServiceClient.groups()
                            .byGroupId(resourceGroupMembership.getAzureGroupRef())
                            .members().ref().post(referenceMember);

                    log.info("UserId: {} added to GroupId: {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());

                    azureGroupMembershipProducerService.addMembership(new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject));

                    log.info("Produced message to Kafka on added UserId {} to GroupId {}",
                            resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());

                } catch (ApiException e) {
                    if (e.getResponseStatusCode() == 400) {
                        if (e.getMessage().contains("object references already exist")) {
                            azureGroupMembershipProducerService.addMembership(new AzureGroupMembership(resourceGroupMembership.getAzureGroupRef(), directoryObject));
                            log.info("Republished to Kafka, UserId {} already added to GroupId {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                            return;
                        }
                        if (e.getMessage().contains("Request_ResourceNotFound")) {
                            log.warn("AzureGroupRef is not correct on user ObjectId {} and group ObjectId {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                            return;
                        }
                        log.warn("Bad request: userRef: {} - groupRef: {}", resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                        log.warn(e.getMessage());
                    }

                    if (e.getResponseStatusCode() == 404) {
                        log.warn("UserId: {} cannot be added to GroupId: {}. GroupId and/or UserId is not found in tenant",
                                resourceGroupMembership.getAzureUserRef(), resourceGroupMembership.getAzureGroupRef());
                        return;
                    }
                    if (e.getResponseStatusCode() == 429) {
                        log.warn("Throttling limit. Error: {}", e.getMessage());
                    } else {
                        log.error("HTTP Error while updating group {}: {} \r",
                                resourceGroupMembership.getAzureGroupRef(), e.getMessage());
                    }
                }
            }, groupExecutor).exceptionally(ex -> {
                log.error("Exception while adding user to group: {}", ex.getMessage(), ex);
                return null;
            });
        }
    }

    public void deleteGroupMembership(String resourceGroupMembershipKey) {
        String[] splitString = resourceGroupMembershipKey.split("_");
        if (splitString.length != 2) {
            log.error("Key on kafka object {} not formatted correctly. NOT deleting membership from group", resourceGroupMembershipKey);
            return;
        }
        String groupId = splitString[0];
        String userId = splitString[1];
        HashKey membershipKey = CoreMembershipMapper.toCoreMembershipHashKey(UUID.fromString(userId), Long.getLong(groupId));

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Trying to remove UserId: {} from GroupId: {} in Graph", userId, groupId);

                graphServiceClient.groups()
                        .byGroupId(groupId)
                        .members()
                        .byDirectoryObjectId(userId)
                        .ref()
                        .delete();

                log.info("UserId: {} removed from GroupId: {}", userId, groupId);

                azureGroupMembershipProducerService.publishDeletedMembership(resourceGroupMembershipKey);
                orchestrator.getMemberships().remove(membershipKey);
                log.info("Produced message to Kafka on deleted UserId: {} from GroupId: {}", userId, groupId);

            } catch (ApiException e) {
                if (e.getResponseStatusCode() == 404) {
                    log.warn("User {} not found in group {}", userId, groupId);

                    azureGroupMembershipProducerService.publishDeletedMembership(resourceGroupMembershipKey);
                    orchestrator.getMemberships().remove(membershipKey);
                    log.warn("Produced message to Kafka on deleted UserId: {} from GroupId: {} as user not found in group", userId, groupId);

                } else {
                    log.error("HTTP Error while trying to remove user {} from group {}. Exception: {} \r{}",
                            userId, groupId, e.getResponseStatusCode(), e.getMessage());
                }
            } catch (Exception e) {
                log.error("Failed to process function deleteGroupMembership, Error: ", e);
            }
        }, groupExecutor).exceptionally(ex -> {
            log.error("Exception while trying to remove user from group: {}", ex.getMessage(), ex);
            return null;
        });
    }
}
