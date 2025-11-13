package no.fintlabs.core.persistence;

import no.fintlabs.core.CoreObjectEvent;
import java.util.List;
import reactor.core.publisher.Mono;

public class MSGraphPersistenceService {

    public Mono<Void> persist(List<CoreObjectEvent> batch) {
        // Call Entra ID API here (non-blocking)
        System.out.println("Persisting batch to Entra ID: " + batch.size() + " items");
        return Mono.empty(); // Replace with actual API call
    }
}