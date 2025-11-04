package no.fintlabs.db;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.fintlabs.db.entity.DBObject;

import java.util.concurrent.ConcurrentHashMap;

@Setter
@Getter
@RequiredArgsConstructor
public class DBObjectList<T extends DBObject> {
    private ConcurrentHashMap<String, T> hashMap = new ConcurrentHashMap<>();
    private DBSink<String, T> sink = new DBSink<>();

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
        T ret = hashMap.put(key, obj);
        persist(key, obj);
        return ret;
    }
    public T putIfAbsent(String key, T obj) {
        T ret = hashMap.putIfAbsent(key, obj);
        if (ret == null) {
            persist(obj);
        }
        return ret;
    }
    private void persist(String key, T obj) {
        sink.persist(key, obj);
    }
    public void loadFromDB() {};
}