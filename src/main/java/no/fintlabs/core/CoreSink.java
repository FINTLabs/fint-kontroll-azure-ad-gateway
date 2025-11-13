package no.fintlabs.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.entity.CoreObject;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Setter
@Getter
@RequiredArgsConstructor
public class CoreSink<I, T extends CoreObject> {
    // To support turning the sink on and off
    private AtomicBoolean enabled = new AtomicBoolean(true);

    //private final Sinks.Many<Tuple2<I, T>> sink = Sinks.many().unicast().onBackpressureBuffer();

    // Unicast sinks only allow one subscriber. If you ever need multiple consumers (e.g., for logging, metrics, or parallel processing), consider:
    // Sinks.Many<>.multicast().onBackpressureBuffer()
    private final Sinks.Many<CoreObjectEvent<I, T>> sink = Sinks.many().unicast().onBackpressureBuffer();

    public void persist(CoreObjectEvent<I, T> object) {
        if (enabled.get()) {
            sink.tryEmitNext(object);
        }
    }

    public void persist(I key, T object, CoreObjectEventType type) {
        persist(new CoreObjectEvent<>(key, object, type));
    }

    public void flush() {
        sink.tryEmitComplete();
    }
}