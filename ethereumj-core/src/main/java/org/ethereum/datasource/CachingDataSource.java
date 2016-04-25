package org.ethereum.datasource;

import org.ethereum.db.ByteArrayWrapper;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Anton Nashatyrev on 18.02.2016.
 */
public class CachingDataSource implements KeyValueDataSource, Flushable {
    KeyValueDataSource source;

    Map<ByteArrayWrapper, byte[]> cache = new HashMap<>();

    public CachingDataSource(KeyValueDataSource source) {
        this.source = source;
    }

    public void flush() {
        Map<byte[], byte[]> records = new HashMap<>();
        for (Map.Entry<ByteArrayWrapper, byte[]> entry : cache.entrySet()) {
            records.put(entry.getKey().getData(), entry.getValue());
        }
        source.updateBatch(records);
        cache.clear();
    }

    @Override
    public synchronized byte[] get(byte[] key) {
        byte[] bb = cache.get(new ByteArrayWrapper(key));
        if (bb == null) {
            return source.get(key);
        } else {
            return bb;
        }
    }

    @Override
    public synchronized byte[] put(byte[] key, byte[] value) {
        return cache.put(new ByteArrayWrapper(key), value);
    }

    @Override
    public synchronized void delete(byte[] key) {
        cache.remove(new ByteArrayWrapper(key));
        source.delete(key);
    }

    @Override
    public synchronized Set<byte[]> keys() {
        return source.keys(); // TODO correctly
    }

    @Override
    public synchronized void updateBatch(Map<byte[], byte[]> rows) {
        for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void setName(String name) {
        source.setName(name);
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public void init() {
        source.init();
    }

    @Override
    public boolean isAlive() {
        return source.isAlive();
    }

    @Override
    @PreDestroy
    public void close() {
        source.close();
    }
}
