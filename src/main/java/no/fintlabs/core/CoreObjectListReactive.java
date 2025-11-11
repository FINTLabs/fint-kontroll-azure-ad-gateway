package no.fintlabs.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.fintlabs.core.entity.CoreObject;
import reactor.core.publisher.Flux;

@Setter
@Getter
@RequiredArgsConstructor
public class CoreObjectListReactive<I, T extends CoreObject> extends CoreObjectList <I, T> {

    private CoreSink<I, T> sink = new CoreSink<>();

    public Flux<CoreObjectEvent<I, T>> updates() {
        return sink.getSink().asFlux();
    }

    public T put(I key, T obj) {
        T ret = hashMap.put(key, obj);
        if (ret == null) {
            persist(key, obj, CoreObjectEventType.CREATED);
        } else {
            persist(key, obj, CoreObjectEventType.UPDATED);
        }
        return ret;
    }

    public T putIfAbsent(I key, T obj) {
        T ret = hashMap.putIfAbsent(key, obj);
        if (ret == null) {
            persist(key, obj, CoreObjectEventType.CREATED);
        }
        return ret;
    }

    public T remove(I key) {
        T ret = hashMap.remove(key);
        if (ret != null) {
            persist(key, ret, CoreObjectEventType.DELETED);
        }
        return ret;
    }

    private void persist(I key, T obj, CoreObjectEventType type) {
        sink.persist(key, obj, type);
    }
}
