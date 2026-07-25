package com.eneik.generated.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.util.concurrent.Callable;

public class FailSafeCache implements Cache {
    private static final Logger log = LoggerFactory.getLogger(FailSafeCache.class);
    private static final String MSG_NATIVE_CACHE_FAILED = "FailSafeCache primary getNativeCache failed, using fallback";
    private static final String MSG_GET_FAILED = "FailSafeCache primary get failed for key: {}, using fallback";
    private static final String MSG_GET_TYPE_FAILED = "FailSafeCache primary get with type failed for key: {}, using fallback";
    private static final String MSG_GET_LOADER_FAILED = "FailSafeCache primary get with loader failed for key: {}, using fallback";
    private static final String MSG_PUT_FAILED = "FailSafeCache primary put failed for key: {}, using fallback";
    private static final String MSG_PUT_IF_ABSENT_FAILED = "FailSafeCache primary putIfAbsent failed for key: {}, using fallback";
    private static final String MSG_EVICT_FAILED = "FailSafeCache primary evict failed for key: {}, using fallback";
    private static final String MSG_EVICT_IF_PRESENT_FAILED = "FailSafeCache primary evictIfPresent failed for key: {}, using fallback";
    private static final String MSG_CLEAR_FAILED = "FailSafeCache primary clear failed, using fallback";
    private static final String MSG_INVALIDATE_FAILED = "FailSafeCache primary invalidate failed, using fallback";

    private final Cache primary;
    private final Cache fallback;
    private final String name;

    public FailSafeCache(Cache primary, String name) {
        this.primary = primary;
        this.name = name;
        this.fallback = new ConcurrentMapCache(name, new BoundedConcurrentMap<>(), true);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        try {
            if (primary != null) {
                return primary.getNativeCache();
            }
        } catch (Exception e) {
            log.warn(MSG_NATIVE_CACHE_FAILED, e);
        }
        return fallback.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        try {
            if (primary != null) {
                return primary.get(key);
            }
        } catch (Exception e) {
            log.warn(MSG_GET_FAILED, key, e);
        }
        return fallback.get(key);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        try {
            if (primary != null) {
                return primary.get(key, type);
            }
        } catch (Exception e) {
            log.warn(MSG_GET_TYPE_FAILED, key, e);
        }
        return fallback.get(key, type);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        try {
            if (primary != null) {
                return primary.get(key, valueLoader);
            }
        } catch (Exception e) {
            log.warn(MSG_GET_LOADER_FAILED, key, e);
        }
        return fallback.get(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        try {
            if (primary != null) {
                primary.put(key, value);
                return;
            }
        } catch (Exception e) {
            log.warn(MSG_PUT_FAILED, key, e);
        }
        fallback.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        try {
            if (primary != null) {
                return primary.putIfAbsent(key, value);
            }
        } catch (Exception e) {
            log.warn(MSG_PUT_IF_ABSENT_FAILED, key, e);
        }
        return fallback.putIfAbsent(key, value);
    }

    @Override
    public void evict(Object key) {
        try {
            if (primary != null) {
                primary.evict(key);
                return;
            }
        } catch (Exception e) {
            log.warn(MSG_EVICT_FAILED, key, e);
        }
        fallback.evict(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        try {
            if (primary != null) {
                return primary.evictIfPresent(key);
            }
        } catch (Exception e) {
            log.warn(MSG_EVICT_IF_PRESENT_FAILED, key, e);
        }
        return fallback.evictIfPresent(key);
    }

    @Override
    public void clear() {
        try {
            if (primary != null) {
                primary.clear();
                return;
            }
        } catch (Exception e) {
            log.warn(MSG_CLEAR_FAILED, e);
        }
        fallback.clear();
    }

    @Override
    public boolean invalidate() {
        try {
            if (primary != null) {
                return primary.invalidate();
            }
        } catch (Exception e) {
            log.warn(MSG_INVALIDATE_FAILED, e);
        }
        return fallback.invalidate();
    }
}
