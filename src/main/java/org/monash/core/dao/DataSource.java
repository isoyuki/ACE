package org.monash.core.dao;

import java.util.List;
import java.util.Map;

/**
 * The data source interface for key-value
 * pair storage.
 */
public interface DataSource {

    boolean insert(String key, String... value);
    boolean insert(byte[] key, byte[]... value);
    void set(byte[] key, byte[] value);
    byte[] get(byte[] key);
    void mset(byte[] key, Map<byte[], byte[]> valueMap);
    List<byte[]> mget(byte[] key, byte[]... field);
    boolean isAvailable(String tableName);
    void close();
}
