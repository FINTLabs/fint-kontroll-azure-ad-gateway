package no.fintlabs.db;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.concurrent.ConcurrentHashMap;

@Setter
@Getter
@RequiredArgsConstructor
public class DBObjectList<T extends DBObject> {
    private ConcurrentHashMap<String, T> hashMap = new ConcurrentHashMap<>();

    private int kafkaOffset = 0;
    public void clear() {
        hashMap.clear();
    }
    public boolean isEmpty() {
        return hashMap.isEmpty();
    }
    public T remove(String key) {
        return hashMap.remove(key);
    }
    public boolean containsKey(String key) {
        return hashMap.containsKey(key);
    }
    public T get(String key) {
        return hashMap.get(key);
    }
    public T put(String key, T obj) {
        return hashMap.put(key, obj);
    }
    public T putIfAbsent(String key, T obj) {
        return hashMap.putIfAbsent(key, obj);
    }
}