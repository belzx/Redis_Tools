package com.lizhi.service.impl;

import com.lizhi.service.IRedisBloomFilter;
import com.lizhi.service.IRedisService;
import com.lizhi.utils.PipelineTemplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Service
public class RedisService implements IRedisService {

    private static Logger log = LoggerFactory.getLogger(RedisService.class);


    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public void remove(final String... keys) {
        for (String key : keys) {
            remove(key);
        }
    }


    @Override
    public void removePattern(final String keys) {
        Set<Serializable> deletedKeys = redisTemplate.keys(keys);
        if (deletedKeys.size() > 0)
            redisTemplate.delete(keys);
    }


    @Override
    public void remove(final String key) {
        if (exists(key)) {
            redisTemplate.delete(key);
        }
    }


    @Override
    public boolean exists(final String key) {
        return redisTemplate.hasKey(key);
    }


    @Override
    public Object get(final String key) {
        return redisTemplate.opsForValue().get(key);
    }


    @Override
    public boolean set(final String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis Failed to set", e);
            return false;
        }
    }


    @Override
    public boolean set(final String key, final Object value, final Long expireTime) {
        SessionCallback<Boolean> sessionCallback = new SessionCallback<Boolean>() {
            @SuppressWarnings("unchecked")
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                redisTemplate.opsForValue().set(key, value);
                redisTemplate.expire(key, expireTime, TimeUnit.MILLISECONDS);
                List<Object> exec = operations.exec();
                if (exec.size() > 0) {
                    return (Boolean) exec.get(0);
                }
                return false;
            }
        };
        return (Boolean) redisTemplate.execute(sessionCallback);
    }


    @Override
    public boolean hmset(String key, Map<String, String> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.error("Failed to hmset", e);
        }
        return false;
    }


    @Override
    public Map<String, String> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }


    @Override
    public boolean lset(String key, Object value, long expireTime, TimeUnit timeUnit) {
        return (Boolean) redisTemplate.execute(new SessionCallback<Boolean>() {
            @Override
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                redisTemplate.opsForList().leftPush(key, value);
                redisTemplate.expire(key, expireTime, timeUnit);
                List<Object> exec = operations.exec();
                if (exec.size() > 0) {
                    return (Boolean) exec.get(0);
                }
                return false;
            }

            ;
        });
    }


    @Override
    public boolean lset(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            log.error("Failed to lset", e);
            return false;
        }
    }


    @Override
    public List<Object> lget(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    @Override
    public Object lgetAll(String key, long start, long end) {
        List<Object> o = new ArrayList();
        Object lget;
        while ((lget = lget(key, start, end)) != null) {
            o.add(lget);
        }
        return o;
    }


    @Override
    public void sendMessage(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
    }


    @Override
    public IRedisBloomFilter getBloomFilter(double falsePositiveProbability, int expectedNumberOfElements) {
        RedisBloomFilter redisBloomFilter = new RedisBloomFilter(falsePositiveProbability, expectedNumberOfElements);
        redisBloomFilter.setRedisService(this);
        return redisBloomFilter;
    }


    @Override
    public IRedisBloomFilter getBloomFilter() {
        RedisBloomFilter redisBloomFilter = new RedisBloomFilter();
        redisBloomFilter.setRedisService(this);
        return redisBloomFilter;
    }


    @Override
    public boolean setBit(String key, long index, boolean value) {
        try {
            return redisTemplate.opsForValue().setBit(key, index, value);
        } catch (Exception e) {
            log.error("Redis Failed to setBit", e);
            return false;
        }
    }


    @Override
    public boolean setBit(final String key, final long index, final boolean value, final long expireTime, final TimeUnit timeUnit) {
        return (Boolean) redisTemplate.execute(new SessionCallback<Boolean>() {
            @SuppressWarnings("unchecked")
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                redisTemplate.opsForValue().setBit(key, index, value);
                redisTemplate.expire(key, expireTime, timeUnit);
                List<Object> exec = operations.exec();
                if (exec.size() > 0) {
                    return (Boolean) exec.get(0);
                }
                return false;
            }
        });
    }


    @Override
    public boolean getBit(String key, long index) {
        return redisTemplate.opsForValue().getBit(key, index);
    }


    @Override
    public boolean expire(String key, Long expireTime, TimeUnit timeUnit) {
        try {
            return redisTemplate.expire(key, expireTime, timeUnit);
        } catch (Exception e) {
            log.error("Redis Failed to expire", e);
            return false;
        }
    }


    @Override
    public Long getExpireTime(String key) {
        return redisTemplate.getExpire(key);
    }


    @Override
    public boolean setIfAbsent(String key, Object value) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value);
        } catch (Exception e) {
            log.error("Failed to setIfAbsent", e);
            return false;
        }
    }


    @Override
    public boolean setIfAbsent(String key, Object value, long expireTime, TimeUnit timeUnit) {
        return (Boolean) redisTemplate.execute(new SessionCallback<Boolean>() {
            @SuppressWarnings("unchecked")
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                redisTemplate.opsForValue().setIfAbsent(key, value);
                redisTemplate.expire(key, expireTime, timeUnit);
                List<Object> exec = operations.exec();
                if (exec.size() > 0) {
                    return (Boolean) exec.get(0);
                }
                return false;
            }
        });
    }

    @Override
    public List<Object> pipeline(PipelineTemplete pipelineTemplete) {
        return ( List<Object>) redisTemplate.execute(new RedisCallback< List<Object>>() {
            @Override
            public  List<Object> doInRedis(RedisConnection connection) throws DataAccessException {
                connection.openPipeline();
                pipelineTemplete.pipelineExecute();
                List<Object> objects = connection.closePipeline();
                pipelineTemplete.resultProcess(objects);
                return objects;
            }
        });
    }
}