package com.eneik.generated.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class FailSafeCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final ConcurrentHashMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public FailSafeCacheManager(CacheManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, n -> {
            Cache primary = delegate != null ? delegate.getCache(n) : null;
            return new FailSafeCache(primary, n);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate != null ? delegate.getCacheNames() : cacheMap.keySet();
    }
}
