package org.ethereum.datasource;

import org.ethereum.util.ByteArrayMap;

import java.util.Map;

/**
 * Created by Anton Nashatyrev on 07.10.2016.
 */
public abstract class MultiCache<V extends CachedSource> extends CachedSourceImpl.BytesKey<V> {

    Map<byte[], V> ownCaches = new ByteArrayMap<>();

    public MultiCache(Source<byte[], V> src) {
        super(src);
    }

    @Override
    public synchronized V get(byte[] key) {
        V ownCache = ownCaches.get(key);
        if (ownCache == null) {
            V v = src != null ? super.get(key) : null;
            ownCache = create(key, v);
            ownCaches.put(key, ownCache);
        }
        return ownCache;
    }

    @Override
    public synchronized boolean flush() {
        boolean ret = false;
        for (Map.Entry<byte[], V> vEntry : ownCaches.entrySet()) {

            if (vEntry.getValue().getSrc() != null) {
                ret |= flushChild(vEntry.getValue());
            } else {
                src.put(vEntry.getKey(), vEntry.getValue());
                ret = true;
            }
        }
        return ret;
    }

    protected boolean flushChild(V childCache) {
        return childCache.flush();
    }

    protected abstract V create(byte[] key, V srcCache);
}
