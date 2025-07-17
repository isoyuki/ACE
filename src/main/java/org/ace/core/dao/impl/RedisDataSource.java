package org.ace.core.dao.impl;

import org.ace.core.dao.DataSource;
import org.ace.crypto.util.ByteArrayKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.Map;

public final class RedisDataSource implements DataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDataSource.class);
    private static final String HOST = "localhost";
    private final JedisPool pool;

    private Transaction transaction;

    public RedisDataSource() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(128);
        pool = new JedisPool(config, HOST, 6379, 0);
        LOGGER.debug("Connection established");
    }


    @Override
    public void initialiseMulti() {
        transaction = pool.getResource().multi();
    }

    @Override
    public void hset_transaction(byte[] key, byte[] field, byte[] value) {
        transaction.hset(key, field, value);
    }

    @Override
    public void hsetnx_transaction(byte[] key, byte[] field, byte[] value) {
        transaction.hsetnx(key, field, value);
    }

    @Override
    public void executeMulti() {
        transaction.exec();
        transaction.close();
    }


    public boolean insert(String key, String... value) {
        try (Jedis jedis = pool.getResource()){
            if(jedis.sadd(key, value) == 1) {
                return true;
            }
        }
        return false;
    }

    public boolean insert(byte[] key, byte[]... value) {
        try (Jedis jedis = pool.getResource()){
            if(jedis.sadd(key, value) == 1) {
                return true;
            }
        }
        return false;
    }

    public void set(byte[] key, byte[] value) {
        try (Jedis jedis = pool.getResource()){
            jedis.set(key, value);
        }
    }

    public byte[] get(byte[] key) {
        try (Jedis jedis = pool.getResource()){
            return jedis.get(key);
        }
    }


    public void hset_map(byte[] key, Map<byte[], byte[]> valueMap) {
        try (Jedis jedis = pool.getResource()){
            jedis.hset(key, valueMap);
        }
    }

    @Override
    public void hset(byte[] key, byte[] field, byte[] value) {
        try (Jedis jedis = pool.getResource()){
            jedis.hset(key, field, value);
        }
    }

    @Override
    public void hsetnx(byte[] key, byte[] field, byte[] value) {
        try (Jedis jedis = pool.getResource()){
            jedis.hsetnx(key, field, value);
        }
    }

    @Override
    public List<byte[]> mget(byte[] key, byte[]... field) {
        try (Jedis jedis = pool.getResource()){
            return jedis.hmget(key, field);
        }
    }

    public long hsetnx_map(byte[] key, Map<byte[], byte[]> valueMap) {
        try (Jedis jedis = pool.getResource()) {

            for (Map.Entry<byte[], byte[]> entry : valueMap.entrySet()) {
                byte[] field = entry.getKey();
                byte[] value = entry.getValue();
                jedis.hsetnx(key, field, value);
            }

            return 1;
        }
    }

    public long hsetnx_ByteKey(byte[] key, Map<ByteArrayKey, byte[]> valueMap) {
        try (Jedis jedis = pool.getResource()) {

            for (Map.Entry<ByteArrayKey, byte[]> entry : valueMap.entrySet()) {
                byte[] field = entry.getKey().getData();
                byte[] value = entry.getValue();
                jedis.hsetnx(key, field, value);
            }
            return 1;
        }
    }

    @Override
    public void hset_ISet(ByteArrayKey key, byte[] value) {
        try (Jedis jedis = pool.getResource()) {

            jedis.hsetnx("ISet".getBytes(), key.getData(), value);

        }
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        try (Jedis jedis = pool.getResource()){
            return jedis.hget(key, field);
        }
    }

    @Override
    public Map<byte[], byte[]> hget_all(byte[] key) {
        try (Jedis jedis = pool.getResource()){
            return jedis.hgetAll(key);
        }
    }

    @Override
    public long hdel(byte[] key, byte[] field) {
        try (Jedis jedis = pool.getResource()){
            return jedis.hdel(key, field);
        }
    }

    @Override
    public void flushAll() {
        try (Jedis jedis = pool.getResource()){
            jedis.flushAll();
        }
    }

    @Override
    public boolean isAvailable(String tableName) {
        // Table is not available in Redis
        return false;
    }

    @Override
    public void close() {
        // Only call when exit
        pool.close();
    }
}
