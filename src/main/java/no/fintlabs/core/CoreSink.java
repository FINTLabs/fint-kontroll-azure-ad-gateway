package no.fintlabs.core;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.entity.CoreObject;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Setter
@Getter
@AllArgsConstructor
public class CoreSink<I, T extends CoreObject> {
    private final Sinks.Many<Tuple2<I, T>> sink = Sinks.many().unicast().onBackpressureBuffer();

    public void persist(Tuple2<I, T> object) {
        sink.tryEmitNext(object);
    }

    public void persist(I key, T object) {
        sink.tryEmitNext(Tuples.of(key, object));
    }

    @PostConstruct
    private void init() {
        sink.asFlux()
                .parallel(20)
                .runOn(Schedulers.boundedElastic())
                .subscribe(t -> updateDatabase(t.getT1(), t.getT2()));
    }

    public void updateDatabase(I key, T object) {
        log.info("Update function being called");
    }
}