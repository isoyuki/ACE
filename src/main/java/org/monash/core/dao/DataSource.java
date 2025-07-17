package org.monash.core.dao;

import org.monash.crypto.util.ByteArrayKey;

import java.util.ArrayList;
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

    byte[] hget(byte[] key, byte[] field);
    Map<byte[], byte[]> hget_all(byte[] key);

    boolean isAvailable(String tableName);
    void close();

    void hdel(byte[] bytes, byte[] l);
}
