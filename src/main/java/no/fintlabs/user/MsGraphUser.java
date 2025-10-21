package no.fintlabs.user;

import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.serialization.UntypedObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azure.*;
import no.fintlabs.config.Config;
import no.fintlabs.config.ConfigUser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@RequiredArgsConstructor

public class MsGraphUser {

    protected final Config config;
    protected final ConfigUser configUser;
    protected final GraphServiceClient graphServiceClient;
    private final ConcurrentHashMap<String, AzureUser> entraIdUserCache;
    private final ConcurrentHashMap<String, AzureUserExternal> entraIdExternalUserCache;
    private final AzureUserProducerService azureUserProducerService;
    private final AzureUserExternalProducerService azureUserExternalProducerService;
    private final ExecutorService userExecutor = Executors.newFixedThreadPool(4);

    private String odataUserDeltaLink;

    @Scheduled(cron = "${fint.kontroll.azure-ad-gateway.user-scheduler.clear-cache}")
    public void clearCaches() {
        entraIdUserCache.clear();
        entraIdExternalUserCache.clear();
        log.info("Delta caches for user has been reset to null due to scheduler. Next call will try to fetch all users and groups from Entra ID");
    }


    @Scheduled(
            initialDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.initial-delay-ms}",
            fixedDelayString = "${fint.kontroll.azure-ad-gateway.user-scheduler.pull.fixed-delay-ms}"
    )
    public void pullAllUsersDelta() {
        log.info("*** <<< Starting to pull users from Microsoft Graph >>> ***");
        final long startTime = System.currentTimeMillis();

        String[] selection = configUser.userAttributesDelta();

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        if (odataUserDeltaLink != null && !odataUserDeltaLink.isBlank()) {
                            return graphServiceClient.users()
                                    .delta()
                                    .withUrl(odataUserDeltaLink)
                                    .get();
                        } else {
                            return graphServiceClient.users()
                                    .delta()
                                    .get(req -> {
                                        req.queryParameters.select = selection;
                                        req.queryParameters.top = configUser.getUserpagingsize();
                                    });
                        }
                    } catch (ApiException e) {
                        throw new CompletionException(e);
                    }
                }, userExecutor)
                .whenComplete((firstPage, ex) -> {
                    try {
                        if (ex != null) {
                            final Throwable t = (ex instanceof CompletionException && ex.getCause() != null)
                                    ? ex.getCause() : ex;
                            if (t instanceof ApiException ae) {
                                log.error("pullAllUsers failed with ApiException message: {}", ae.getMessage());
                            } else {
                                log.error("pullAllUsers failed: {}", t.toString());
                            }
                            return;
                        }

                        pageThroughUsersDelta(firstPage);

                    } catch (RuntimeException e1) {
                        log.error("Failed during paging/processing: {}", e1.getMessage());
                    } finally {
                        final long elapsed = System.currentTimeMillis() - startTime;
                        final long minutes = (elapsed / 1000) / 60;
                        final long seconds = (elapsed / 1000) % 60;
                        log.info("*** <<< Finished pulling users from Microsoft Graph in {} minutes and {} seconds >>> *** ",
                                minutes, seconds);
                    }
                });
    }

    private void pageThroughUsersDelta(com.microsoft.graph.users.delta.DeltaGetResponse firstPage) {
        final AtomicInteger users = new AtomicInteger();
        final AtomicInteger changedUsers = new AtomicInteger();
        final AtomicInteger changedExtUsers = new AtomicInteger();

        com.microsoft.graph.users.delta.DeltaGetResponse lastPage = firstPage;

        final Set<String> seenNextLinks = new HashSet<>();

        for (com.microsoft.graph.users.delta.DeltaGetResponse current = firstPage;
             current != null;
             current = Optional.ofNullable(current.getOdataNextLink())
                     .map(next -> {
                         if (!seenNextLinks.add(next)) {
                             log.error("Detected nextLink cycle; stopping paging. nextLink={}", next);
                             return null;
                         }
                         return graphServiceClient.users()
                                 .delta()
                                 .withUrl(next)
                                 .get(req -> req.headers.add("Prefer", "return=minimal"));
                     })
                     .orElse(null)) {

            if (current.getValue() != null) {
                for (User u : current.getValue()) {
                    processUserItem(u, users, changedUsers, changedExtUsers);
                }
            }
            lastPage = current;
        }

        final String newDelta = (lastPage != null) ? lastPage.getOdataDeltaLink() : null;
        if (newDelta == null || newDelta.isBlank()) {
            log.error("Logic error: Last user page doesn't contain @odata.deltaLink");
            return;
        }

        final boolean initialRun = (odataUserDeltaLink == null);
        odataUserDeltaLink = newDelta;
        if (initialRun) {
            log.info("*** <<< Initial Delta run on Users completed >>> ***");
        }

        if (!initialRun) {
            if (changedUsers.get() > 0) {
                log.info("*** <<< Found total of {} scoped users in Entra ID. Published {} changed users to Kafka >>> ***",
                        users.get(), changedUsers.get());
            } else {
                log.info("*** <<< No changes since last call on users from Graph >>> ***");
            }
            if (changedExtUsers.get() > 0) {
                log.info("*** <<< Found {} External users that changed >>> ***", changedExtUsers.get());
            }
        } else {
            if (changedUsers.get() > 0) {
                log.info("*** <<< Found total of {} scoped users in Entra ID. {} published to Kafka >>> ***",
                        users.get(), changedUsers.get());
            } else {
                log.info("*** <<< No changes since initial call >>> ***");
            }
            if (changedExtUsers.get() > 0) {
                log.info("*** <<< Of the total, {} were External users >>> ***", changedExtUsers.get());
            } else {
                log.info("*** <<< No External user changes since initial call >>> ***");
            }
        }
    }

    private void processUserItem(
            User user,
            AtomicInteger users,
            AtomicInteger changedUsers,
            AtomicInteger changedExtUsers
    ) {
        Map<String, Object> ad = user.getAdditionalData();
        if (ad != null && ad.containsKey("@removed")) {
            String userId = user.getId();
            handleUserDeleted(userId);
            return;
        }

        if (user.getUserType() == null || !user.getUserType().equalsIgnoreCase("Member")) return;



        if (entraIdUserCache != null) {
            final AzureUser cached = entraIdUserCache.get(user.getId());
            if (cached != null) {
                final AzureUser fresh = new AzureUser(user, configUser);
                if (fresh.equals(cached)) {
                    log.debug("User {} unchanged. Skipping Kafka.", user.getId());
                    return;
                }
            }
        }

        final String externalUserAttribute =
                AzureUser.getAttributeValue(user, configUser.getExternaluserattribute());
        if (Boolean.TRUE.equals(configUser.getEnableExternalUsers())
                && externalUserAttribute != null
                && externalUserAttribute.equalsIgnoreCase(configUser.getExternaluservalue())) {
            users.incrementAndGet();
            final AzureUserExternal ext = new AzureUserExternal(user, configUser);
            if (entraIdExternalUserCache != null) {
                final AzureUserExternal cachedExt = entraIdExternalUserCache.get(user.getId());
                if (ext.equals(cachedExt)) {
                    log.info("External user {} unchanged. Skipping Kafka.", user.getId());
                    return;
                }
                entraIdExternalUserCache.put(user.getId(), ext);
            }
            log.debug("Publishing external user to Kafka: {}", user.getUserPrincipalName());
            azureUserExternalProducerService.publish(ext);
            changedExtUsers.incrementAndGet();
            return;
        }

        final AzureUser az = new AzureUser(user, configUser);
        if ((az.getEmployeeId() != null && !az.getEmployeeId().isEmpty())
                || (az.getStudentId() != null && !az.getStudentId().isEmpty())) {
            users.incrementAndGet();
            log.debug("Publishing user to Kafka: {}", user.getUserPrincipalName());
            azureUserProducerService.publish(az);
            log.debug("Updating cache for user: {}", user.getId());
            changedUsers.incrementAndGet();
            if (entraIdUserCache != null) {
                entraIdUserCache.put(user.getId(), az);
            }
        } else {
            log.debug("UserId: {} is missing employeeId/studentId. Not published to kafka.", user.getId());
        }
    }
    private void handleUserDeleted(String userId) {
        if (entraIdUserCache != null) {
            entraIdUserCache.remove(userId);
            azureUserProducerService.publishDeletedUser(userId);
        }
        if (entraIdExternalUserCache != null) {
            entraIdExternalUserCache.remove(userId);
            azureUserExternalProducerService.publishDeletedUser(userId);
        }

        log.info("Detected deletion of user {} via delta; caches updated and deletion event published.", userId);
    }

}
