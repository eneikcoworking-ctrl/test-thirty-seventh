package com.eneik.generated.config;

import java.util.concurrent.ConcurrentHashMap;

public class BoundedConcurrentMap<K, V> extends ConcurrentHashMap<K, V> {
    private static final long serialVersionUID = 1L;
    private static final int MAX_SIZE = 1000;

    @Override
    public V put(K key, V value) {
        if (size() >= MAX_SIZE && !containsKey(key)) {
            clear();
        }
        return super.put(key, value);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        if (size() >= MAX_SIZE && !containsKey(key)) {
            clear();
        }
        return super.putIfAbsent(key, value);
    }
}
