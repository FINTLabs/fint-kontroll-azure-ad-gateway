package no.fintlabs.core;

import no.fintlabs.TestUtils;
import no.fintlabs.azure.HashKey;
import no.fintlabs.core.entity.CoreDevice;
import no.fintlabs.core.entity.CoreUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoreObjectListReactiveTest {

    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static final int CONCURRENCY = Math.min(CORES * 8, 256);

    void removeNRandomUsersFromList(CoreObjectListReactive<UUID, CoreUser> userList, int nUsers) {
        Map<UUID, CoreUser> randomUsers = TestUtils.pickNRandomUsers(userList, nUsers);
        randomUsers.forEach((uuid, user) -> userList.remove(uuid));
    }

    void removeNRandomDevicesFromList(CoreObjectListReactive<UUID, CoreDevice> deviceList, int nDevices) {
        Map<UUID, CoreDevice> randomDevices = TestUtils.pickNRandomDevices(deviceList, nDevices);
        randomDevices.forEach((uuid, device) -> deviceList.remove(uuid));
    }

    void updateNRandomUsersInLIst(CoreObjectListReactive<UUID, CoreUser> userList, int nUsers) {
        Map<UUID, CoreUser> randomUsers = TestUtils.pickNRandomUsers(userList, nUsers);
        randomUsers.forEach((uuid, user) -> userList.put(uuid, TestUtils.getRandomUser()));
    }

    void updateNRandomDevicesInLIst(CoreObjectListReactive<UUID, CoreDevice> deviceList, int nDevices) {
        Map<UUID, CoreDevice> randomDevices = TestUtils.pickNRandomDevices(deviceList, nDevices);
        randomDevices.forEach((uuid, device) -> deviceList.put(uuid, TestUtils.getRandomDevice()));
    }

    @Test
    void makeSure5newUsersGetsProcessedAsync5Times() {

        class Counter {
            void record(CoreObjectEventType type) {
            }
        }
        Counter counter = Mockito.spy(new Counter());

        CoreObjectListReactive<UUID, CoreUser> userList = new CoreObjectListReactive<>();
        TestUtils.Outputter outputter = Mockito.spy(new TestUtils.Outputter());

        userList.updates().flatMap(
                        userEvent ->
                                Mono.fromCallable(() -> {
                                    outputter.write(userEvent.getId().toString());
                                    outputter.write(userEvent.getObject().getHash().toString());
                                    outputter.write(userEvent.getType().toString());
                                    counter.record(userEvent.getType());
                                    return userEvent.getId();
                                }).subscribeOn(Schedulers.boundedElastic()),
                        CONCURRENCY,
                        1024
                )
                .onErrorContinue((e, o) -> outputter.writeError("Failed to update Azure. " + e.toString()))
                .subscribe(result -> outputter.write(result.toString()));

        for (int i = 0; i < 5; i++) {
            userList.put(UUID.randomUUID(), new CoreUser(HashKey.createRandomHashKey()));
        }

        await().atMost(2, SECONDS).untilAsserted(() -> {
            verify(counter, times(5)).record(any(CoreObjectEventType.class));
            verify(outputter, never()).writeError(anyString());
        });
    }

    @Test
    void makeSureKnownActionIsDoneLast() {

        class Counter {
            void record(CoreObjectEventType type) {
            }
        }
        Counter counter = Mockito.spy(new Counter());

        CoreObjectListReactive<UUID, CoreUser> userList = new CoreObjectListReactive<>();
        TestUtils.Outputter outputter = Mockito.spy(new TestUtils.Outputter());

        userList.updates().flatMap(
                        userEvent ->
                                Mono.fromCallable(() -> {
                                    outputter.write(userEvent.getId().toString());
                                    outputter.write(userEvent.getObject().getHash().toString());
                                    outputter.write(userEvent.getType().toString());
                                    return userEvent.getId();
                                }).subscribeOn(Schedulers.boundedElastic()),
                        CONCURRENCY,
                        1024
                )
                .onErrorContinue((e, o) -> outputter.writeError("Failed to update Azure. " + e.toString()))
                .doOnComplete(() -> {
                    outputter.write("Processing done!");
                    counter.record(CoreObjectEventType.CREATED);
                })
                .subscribe();

        for (int i = 0; i < 5; i++) {
            userList.put(UUID.randomUUID(), new CoreUser(HashKey.createRandomHashKey()));
        }
        userList.getSink().tryEmitComplete();

        await().atMost(2, SECONDS).untilAsserted(() -> {
            verify(counter, times(1)).record(any(CoreObjectEventType.class));
            verify(counter, times(1)).record(CoreObjectEventType.CREATED);
            verify(outputter, never()).writeError(anyString());
        });
    }


    @Test
    void makeSure101usersAnd100BatchResultsIn2Calls() {

        TestUtils.Outputter outputter = Mockito.spy(new TestUtils.Outputter());
        class Counter {
            void record() {
            }
        }
        Counter counter = Mockito.spy(new Counter());

        CoreObjectListReactive<UUID, CoreUser> userList = new CoreObjectListReactive<>();

        userList.updates()
                .doOnNext(u -> outputter.write("Received: " + u))
                .doOnComplete(() -> outputter.write("Upstream completed"))
                .window(100)
                .doOnNext(w -> outputter.write("New window created"))
                .flatMapSequential(window -> window.collectList()
                                .flatMap(batch -> {
                                    outputter.write("Processing batch " + " with size " + batch.size());
                                    batch.forEach(item -> outputter.write("  -> processed " + item));
                                    counter.record();
                                    return Mono.empty();
                                }),
                        CONCURRENCY,
                        1024
                )
                .onErrorContinue((e, o) -> outputter.writeError("Failed to update Azure. " + e.toString()))
                .doOnComplete(() -> outputter.write("✅ All batches processed"))
                .subscribe(
                        result -> outputter.write(result.toString()),
                        error -> outputter.writeError("Failed to update Azure. " + error.toString())
                );

        TestUtils.addNRandomUsersToList(userList, 101);
        // Signal that the Sink is "finished"
        userList.getSink().tryEmitComplete();

        await().atMost(3, SECONDS).untilAsserted(() -> verify(counter, times(2)).record());
    }

    @Test
    void makeSure20RemovedUsersResultsIn1Page() {

        TestUtils.Outputter outputter = Mockito.spy(new TestUtils.Outputter());
        class Counter {
            void record() {
            }
        }
        Counter counter = Mockito.spy(new Counter());

        CoreObjectListReactive<UUID, CoreUser> userList = new CoreObjectListReactive<>();

        userList.updates()
                .doOnNext(u -> outputter.write("Received: " + u))
                .doOnComplete(() -> outputter.write("Upstream completed"))
                .window(100)
                .doOnNext(w -> outputter.write("New window created"))
                .flatMapSequential(window -> window.collectList()
                                .flatMap(batch -> {
                                    outputter.write("Processing batch " + " with size " + batch.size());
                                    batch.forEach(item -> outputter.write("  -> processed " + item));
                                    counter.record();
                                    return Mono.empty();
                                }),
                        CONCURRENCY,
                        1024
                )
                .onErrorContinue((e, o) -> outputter.writeError("Failed to update Azure. " + e.toString()))
                .doOnComplete(() -> outputter.write("✅ All batches processed"))
                .subscribe(
                        result -> outputter.write(result.toString()),
                        error -> outputter.writeError("Failed to update Azure. " + error.toString())
                );

        userList.getSink().setEnabled(new AtomicBoolean(false));
        TestUtils.addNRandomUsersToList(userList, 101);
        userList.getSink().setEnabled(new AtomicBoolean(true));

        removeNRandomUsersFromList(userList, 20);

        // Signal that the Sink is "finished"
        userList.getSink().tryEmitComplete();

        await().atMost(3, SECONDS).untilAsserted(() -> verify(counter, times(1)).record());
    }

    @Test
    void makeSure10Removed10New10UpdatedUsersResultsIn3DifferentPages() {
        int waitForPageInSeconds = 10;
        TestUtils.Outputter outputter = Mockito.spy(new TestUtils.Outputter());
        @SuppressWarnings("unused")
        class Counter {
            void record(CoreObjectEventType type) {
            }

            void recordItem(CoreObjectEventType type) {
            }
        }
        Counter counter = Mockito.spy(new Counter());

        CoreObjectListReactive<UUID, CoreUser> userList = new CoreObjectListReactive<>();

        userList.updates()
                .doOnNext(u -> outputter.write("Received: " + u))
                .doOnComplete(() -> outputter.write("Upstream completed"))
                .groupBy(CoreObjectEvent::getType)
                .flatMap(groupedFlux ->
                        groupedFlux
                                .windowTimeout(100, Duration.ofSeconds(waitForPageInSeconds))
                                .doOnNext(w -> outputter.write("New window created"))
                                .flatMapSequential(window ->
                                                window.collectList()
                                                        .filter(batch -> !batch.isEmpty())
                                                        .flatMap(batch -> {
                                                            outputter.write("Processing batch with size " + batch.size());
                                                            counter.record(groupedFlux.key());
                                                            batch.forEach(item -> {
                                                                outputter.write("  -> processed " + item);
                                                                counter.recordItem(groupedFlux.key());
                                                            });
                                                            return Mono.empty();
                                                        }),
                                        CONCURRENCY,
                                        1024
                                )
                )
                .onErrorContinue((e, o) -> outputter.writeError("Failed to update Azure. " + e))
                .doOnComplete(() -> outputter.write("✅ All batches processed"))
                .subscribe();

        userList.getSink().setEnabled(new AtomicBoolean(false));
        TestUtils.addNRandomUsersToList(userList, 100);
        userList.getSink().setEnabled(new AtomicBoolean(true));

        TestUtils.addNRandomUsersToList(userList, 10);
        removeNRandomUsersFromList(userList, 10);
        updateNRandomUsersInLIst(userList, 10);

        // Signal that the Sink is "finished"
        userList.getSink().tryEmitComplete();

        await().atMost(3, SECONDS).untilAsserted(() -> {
            verify(counter, times(3)).record(any(CoreObjectEventType.class));
            verify(counter, times(1)).record(CoreObjectEventType.CREATED);
            verify(counter, times(1)).record(CoreObjectEventType.UPDATED);
            verify(counter, times(1)).record(CoreObjectEventType.DELETED);
            verify(counter, times(10)).recordItem(CoreObjectEventType.CREATED);
            verify(counter, times(10)).recordItem(CoreObjectEventType.UPDATED);
            verify(counter, times(10)).recordItem(CoreObjectEventType.DELETED);
        });
    }


    @Test
    void makeSure101RemovedUsersResultsIn2PagesAnd101Deletes() {

        TestUtils.Outputter outputter = Mockito.spy(new TestUtils.Outputter());
        @SuppressWarnings("unused")
        class Counter {
            void recordPage() {
            }
            void recordItem(CoreObjectEventType type) {
            }
        }
        Counter counter = Mockito.spy(new Counter());

        CoreObjectListReactive<UUID, CoreUser> userList = new CoreObjectListReactive<>();

        userList.updates()
                .doOnNext(u -> outputter.write("Received: " + u))
                .doOnComplete(() -> outputter.write("Upstream completed"))
                .window(100)
                .doOnNext(w -> outputter.write("New window created"))
                .flatMapSequential(window -> window.collectList()
                                .flatMap(batch -> {
                                    outputter.write("Processing batch " + " with size " + batch.size());
                                    counter.recordPage();
                                    batch.forEach(item -> {
                                        outputter.write("  -> processed " + item);
                                        counter.recordItem(item.getType());
                                    });
                                    return Mono.empty();
                                }),
                        CONCURRENCY,
                        1024
                )
                .onErrorContinue((e, o) -> outputter.writeError("Failed to update Azure. " + e.toString()))
                .doOnComplete(() -> outputter.write("✅ All batches processed"))
                .subscribe(
                        result -> outputter.write(result.toString()),
                        error -> outputter.writeError("Failed to update Azure. " + error.toString())
                );

        userList.getSink().setEnabled(new AtomicBoolean(false));
        TestUtils.addNRandomUsersToList(userList, 200);
        userList.getSink().setEnabled(new AtomicBoolean(true));

        removeNRandomUsersFromList(userList, 101);

        // Signal that the Sink is "finished"
        userList.getSink().tryEmitComplete();

        await().atMost(3, SECONDS).untilAsserted(() -> {
            verify(counter, times(2)).recordPage();
            verify(counter, times(101)).recordItem(CoreObjectEventType.DELETED);
        });
    }

    @Test
    void makeSure10Removed10New10UpdatedDevicesResultsInSOMETINGSOMETHING() {
        int waitForPageInSeconds = 10;
        TestUtils.Outputter outputter = Mockito.spy(new TestUtils.Outputter());
        @SuppressWarnings("unused")
        class Counter {
            void record(CoreObjectEventType type) {
            }

            void recordItem(CoreObjectEventType type) {
            }
        }
        Counter counter = Mockito.spy(new Counter());

        CoreObjectListReactive<UUID, CoreDevice> deviceList = new CoreObjectListReactive<>();

        deviceList.updates()
                .doOnNext(u -> outputter.write("Received: " + u))
                .doOnComplete(() -> outputter.write("Upstream completed"))
                .groupBy(CoreObjectEvent::getType)
                .flatMap(groupedFlux ->
                        groupedFlux
                                .windowTimeout(100, Duration.ofSeconds(waitForPageInSeconds))
                                .doOnNext(w -> outputter.write("New window created"))
                                .flatMapSequential(window ->
                                                window.collectList()
                                                        .filter(batch -> !batch.isEmpty())
                                                        .flatMap(batch -> {
                                                            outputter.write("Processing batch with size " + batch.size());
                                                            counter.record(groupedFlux.key());
                                                            batch.forEach(item -> {
                                                                outputter.write("  -> processed " + item);
                                                                counter.recordItem(groupedFlux.key());
                                                            });
                                                            return Mono.empty();
                                                        }),
                                        CONCURRENCY,
                                        1024
                                )
                )
                .onErrorContinue((e, o) -> outputter.writeError("Failed to update Azure. " + e))
                .doOnComplete(() -> outputter.write("✅ All batches processed"))
                .subscribe();

        deviceList.getSink().setEnabled(new AtomicBoolean(false));
        TestUtils.addNRandomDevicesToList(deviceList, 100);
        deviceList.getSink().setEnabled(new AtomicBoolean(true));

        TestUtils.addNRandomDevicesToList(deviceList, 10);
        removeNRandomDevicesFromList(deviceList, 10);
        updateNRandomDevicesInLIst(deviceList, 10);

        // Signal that the Sink is "finished"
        deviceList.getSink().tryEmitComplete();

        await().atMost(3, SECONDS).untilAsserted(() -> {
            verify(counter, times(3)).record(any(CoreObjectEventType.class));
            verify(counter, times(1)).record(CoreObjectEventType.CREATED);
            verify(counter, times(1)).record(CoreObjectEventType.UPDATED);
            verify(counter, times(1)).record(CoreObjectEventType.DELETED);
            verify(counter, times(10)).recordItem(CoreObjectEventType.CREATED);
            verify(counter, times(10)).recordItem(CoreObjectEventType.UPDATED);
            verify(counter, times(10)).recordItem(CoreObjectEventType.DELETED);
        });
    }

}