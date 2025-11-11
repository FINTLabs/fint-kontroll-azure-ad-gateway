package no.fintlabs.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

enum CoreObjectEventType {
    CREATED, UPDATED, DELETED
}

@Getter
@RequiredArgsConstructor
public class CoreObjectEvent<I, T> {
    private final I id;
    private final T object;
    private final CoreObjectEventType type;

    @Override
    public String toString() {
        return type.toString() +
                id.toString() +
                object.toString();
    }
}
