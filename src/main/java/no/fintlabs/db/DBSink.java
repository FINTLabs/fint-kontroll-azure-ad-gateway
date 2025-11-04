package no.fintlabs.db;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.db.entity.DBObject;
import no.fintlabs.kafka.ResourceGroupMembership;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

@Slf4j
@Setter
@Getter
@AllArgsConstructor
public class DBSink<I, T extends DBObject> {
    private final Sinks.Many<Tuple2<I, T>> sink = Sinks.many().unicast().onBackpressureBuffer();

    public void persist(Tuple2<I, T> object) {
        sink.tryEmitNext(object);
    }

    @PostConstruct
    private void init() {
        sink.asFlux()
                .parallel(20)
                .runOn(Schedulers.boundedElastic())
                .subscribe(t -> updateDatabase(t.getT1(), t.getT2()));
    }

    public void updateDatabase(I key, DBObject T) {
        log.info("Update function being called");
    }
}