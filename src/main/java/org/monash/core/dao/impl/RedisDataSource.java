package org.monash.core.dao.impl;

import org.monash.core.dao.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.Map;

public final class RedisDataSource implements DataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDataSource.class);
    private static final String HOST = "localhost";
    private final JedisPool pool;

    public RedisDataSource() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(128);
        pool = new JedisPool(config, HOST);
        LOGGER.debug("Connection established");
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

    @Override
    public void mset(byte[] key, Map<byte[], byte[]> valueMap) {
        try (Jedis jedis = pool.getResource()){
            jedis.hmset(key, valueMap);
        }
    }

    @Override
    public List<byte[]> mget(byte[] key, byte[]... field) {
        try (Jedis jedis = pool.getResource()){
            return jedis.hmget(key, field);
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
