package org.ace.core.dao;

import org.ace.crypto.util.ByteArrayKey;

import java.util.List;
import java.util.Map;

/**
 * The data source interface for key-value
 * pair storage.
 */
public interface DataSource {

    void initialiseMulti();
    void hset_transaction(byte[] key, byte[] field, byte[] value);
    void hsetnx_transaction(byte[] key, byte[] field, byte[] value);
    void executeMulti();

    boolean insert(String key, String... value);
    boolean insert(byte[] key, byte[]... value);

    void set(byte[] key, byte[] value);
    byte[] get(byte[] key);
    
    void hset_map(byte[] key, Map<byte[], byte[]> valueMap);
    void hset(byte[] key, byte[] field, byte[] value);
    void hsetnx(byte[] key, byte[] field, byte[] value);

    List<byte[]> mget(byte[] key, byte[]... field);

    long hsetnx_map(byte[] key, Map<byte[], byte[]> valueMap);
    long hsetnx_ByteKey(byte[] key, Map<ByteArrayKey, byte[]> valueMap);

    void hset_ISet(ByteArrayKey key, byte[] value);

    byte[] hget(byte[] key, byte[] field);
    Map<byte[], byte[]> hget_all(byte[] key);

    boolean isAvailable(String tableName);

    void flushAll();

    void close();

    long hdel(byte[] bytes, byte[] l);
}
