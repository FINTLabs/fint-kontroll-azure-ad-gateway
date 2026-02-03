package no.fintlabs.core;

import com.microsoft.graph.models.Schedule;
import lombok.SneakyThrows;
import no.fintlabs.TestUtils;
import no.fintlabs.azure.HashKey;
import no.fintlabs.core.entity.CoreObject;
import no.fintlabs.core.entity.CoreUser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Subscription;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CoreSinkTest {

    @Test
    void MakeSureSinkWaitsIfFull() {


        CoreSink sink = new CoreSink();
        sink.setBlockingQueueSize(1);
        for (int i = 0; i < 100; i++) {
            sink.persist(UUID.randomUUID(), TestUtils.getRandomUser(), CoreObjectEventType.CREATED);
        }
    }

    @Test
    void shouldNotFailWhenBufferIsFull() {
        TestUtils.Outputter outputter = Mockito.spy(new TestUtils.Outputter());
        CoreSink sink = new CoreSink();

        //somechange

        ReflectionTestUtils.setField(sink, "sink", Sinks.many().unicast().onBackpressureBuffer(new ArrayBlockingQueue<>(5)));
        //ReflectionTestUtils.setField(sink, "sink", Sinks.many().unicast().onBackpressureBuffer());
        //ReflectionTestUtils.setField(sink, "sink", Sinks.many().multicast().onBackpressureBuffer(5));

        // Subscriber som IKKE requester -> ingenting dreneres fra køen
//        sink.getSink().asFlux().subscribe(new BaseSubscriber<>() {
//            @Override
//            protected void hookOnSubscribe(Subscription subscription) {
//                System.out.println(subscription);
//            }
//        });


        sink.getSink().asFlux()
                .flatMapSequential( u -> {
                    System.out.println(u);
                    System.out.flush();
                    TestUtils.sleep(1000);
                    return Mono.empty();
                })
                .subscribe();

        System.out.println("Persisting to queue...");
        UUID NewUUID;
        for (int i = 0; i < 10; i++) {
            NewUUID = UUID.randomUUID();
            System.out.println(" PERSISTING event - " + NewUUID.toString());
            sink.persist(NewUUID, TestUtils.getRandomUser(), CoreObjectEventType.UPDATED);
            System.out.println(" .. PERSISTED event - " + NewUUID.toString());
            System.out.flush();
        }
        System.out.println("... done!");

        TestUtils.sleep(2000);

//        assertEquals(Sinks.EmitResult.OK, r1);
//        assertEquals(Sinks.EmitResult.OK, r2);
    }

    @Test
    void persist() {
    }

    @Test
    void testPersist() {
    }

    @Test
    void flush() {
    }

    @Test
    void getEnabled() {
    }

    @Test
    void getSink() {
    }

    @Test
    void setEnabled() {
    }
}