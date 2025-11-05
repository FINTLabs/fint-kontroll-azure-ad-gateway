package no.fintlabs.db;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.fintlabs.db.entity.DBObject;

import java.util.concurrent.ConcurrentHashMap;

@Setter
@Getter
@RequiredArgsConstructor
public class DBObjectList<I, T extends DBObject> {
    private ConcurrentHashMap<I, T> hashMap = new ConcurrentHashMap<>();
    private DBSink<I, T> sink = new DBSink<>();

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
        T ret = hashMap.put(key, obj);
        persist(key, obj);
        return ret;
    }

    public T putIfAbsent(I key, T obj) {
        T ret = hashMap.putIfAbsent(key, obj);
        if (ret == null) {
            persist(key, obj);
        }
        return ret;
    }

    private void persist(I key, T obj) {
        sink.persist(key, obj);
    }

    public void loadFromDB() {};
}