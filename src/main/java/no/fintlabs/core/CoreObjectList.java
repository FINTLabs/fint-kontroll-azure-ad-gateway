package no.fintlabs.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.fintlabs.core.entity.CoreObject;

import java.util.concurrent.ConcurrentHashMap;

@Setter
@Getter
@RequiredArgsConstructor
public class CoreObjectList<I, T extends CoreObject> {
    protected ConcurrentHashMap<I, T> hashMap = new ConcurrentHashMap<>();

    private int kafkaOffset = 0;
    public void clear() {
        hashMap.clear();
    }
    public boolean isEmpty() {
        return hashMap.isEmpty();
    }
    public T remove(I key) {
        return hashMap.remove(key);
    }
    public boolean containsKey(I key) {
        return hashMap.containsKey(key);
    }

    public T get(I key) {
        return hashMap.get(key);
    }

    public int size () {
        return hashMap.size();
    }

    public T put(I key, T obj) {
        return hashMap.put(key, obj);
    }

    public T putIfAbsent(I key, T obj) {
        return hashMap.putIfAbsent(key, obj);
    }

}