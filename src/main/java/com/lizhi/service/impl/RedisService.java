package com.lizhi.service.impl;

import com.lizhi.service.IRedisBloomFilter;
import com.lizhi.service.IRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * redicache 工具类
 */
@Service
public class RedisService implements IRedisService {

    private static Logger log = LoggerFactory.getLogger(RedisService.class);

    private static ThreadLocal<String> threadLocal = new ThreadLocal<>();
    /**
     * redisTemplate.opsForValue();//操作字符串
     * redisTemplate.opsForHash();//操作hash
     * redisTemplate.opsForList();//操作list
     * redisTemplate.opsForSet();//操作set
     * redisTemplate.opsForZSet();//操作有序set
     */
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 批量删除对应的value
     *
     * @param keys
     */
    public void remove(final String... keys) {
        for (String key : keys) {
            remove(key);
        }
    }

    /**
     * 批量删除key
     *
     * @param pattern
     */
    public void removePattern(final String pattern) {
        Set<Serializable> keys = redisTemplate.keys(pattern);
        if (keys.size() > 0)
            redisTemplate.delete(keys);
    }


    /**
     * 删除对应的value
     *
     * @param key
     */
    public void remove(final String key) {
        if (exists(key)) {
            redisTemplate.delete(key);
        }
    }

    public boolean exists(final String key) {
        return redisTemplate.hasKey(key);
    }

    public Object get(final String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis Failed to get", e);
        }
        return null;
    }

    public boolean set(final String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Redis Failed to set", e);
            return false;
        }
        return true;
    }

    /**
     * 使用SessionCallBack这个接口，通过这个接口就可以把属于多个同一套命令放在同一个
     * Redis连接中去执行
     * 通过SessionCallBack 接口可以保证原子性
     * <p>
     * redis的事务是由multi和exec包围起来的部分，当发出multi命令时，redis会进入事务，redis会进入阻塞状态，不再响应任何别的客户端的请求，
     * 直到发出multi命令的客户端再发出exec命令为止。那么被multi和exec包围的命令会进入独享redis的过程，直到执行完毕
     *
     * @param key
     * @param value
     * @param expireTime
     * @return
     */
    public boolean set(final String key, final Object value, final Long expireTime) {
        SessionCallback<Boolean> sessionCallback = new SessionCallback<Boolean>() {
            @SuppressWarnings("unchecked")
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                if (!set(key, value)) {
                    return false;
                }
                if (!expire(key, expireTime, TimeUnit.SECONDS)) {
                    return false;
                }
                List<Object> exec = operations.exec();
                if (exec.size() > 0) {
                    return (Boolean) exec.get(0);
                }
                return false;
            }
        };
        return (Boolean) redisTemplate.execute(sessionCallback);
    }

    public boolean hmset(String key, Map<String, String> value) {
        try {
            redisTemplate.opsForHash().putAll(key, value);
            return true;
        } catch (Exception e) {
            log.error("", e);
        }
        return false;
    }

    public Map<String, String> hmget(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    public void lset(String key, Object object) {
        try {
            redisTemplate.opsForList().leftPush(key, object);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public Object lget(String key) {
        try {
            return redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
    }

    public Object lgetAll(String key) {
        List<Object> o = new ArrayList();
        try {
            Object lget;
            while ((lget = lget(key)) != null) {
                o.add(lget);
            }
            return o;
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
    }

    public void sendMessage(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
    }

    public IRedisBloomFilter getBloomFilter(double falsePositiveProbability, int expectedNumberOfElements) {
        RedisBloomFilter redisBloomFilter = new RedisBloomFilter(falsePositiveProbability, expectedNumberOfElements);
        redisBloomFilter.setRedisService(this);
        return redisBloomFilter;
    }

    public IRedisBloomFilter getBloomFilter() {
        RedisBloomFilter redisBloomFilter = new RedisBloomFilter();
        redisBloomFilter.setRedisService(this);
        return redisBloomFilter;
    }

    public boolean setBit(String key, long index, boolean value) {
        try {
            return redisTemplate.opsForValue().setBit(key, index, value);
        } catch (Exception e) {
            log.error("Redis Failed to setBit");
            return false;
        }
    }

    public boolean setBit(final String key, final long index, final boolean value, final long expireTime, final TimeUnit timeUnit) {
        return (Boolean) redisTemplate.execute(new SessionCallback<Boolean>() {
            @SuppressWarnings("unchecked")
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                if (!setBit(key, index, value)) {
                    return false;
                }
                if (!expire(key, expireTime, timeUnit)) {
                    return false;
                }
                List<Object> exec = operations.exec();
                if (exec.size() > 0) {
                    return (Boolean) exec.get(0);
                }
                return false;
            }
        });
    }

    public boolean getBit(String key, long index) {
        try {
            return redisTemplate.opsForValue().getBit(key, index);
        } catch (Exception e) {
            log.error("Redis Failed to getBit");
            return false;
        }
    }

    public boolean expire(String key, Long expireTime, TimeUnit timeUnit) {
        try {
            redisTemplate.expire(key, expireTime, timeUnit);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Redis Failed to expire", e);
            return false;
        }
    }

    public Long getExpireTime(String key) {
        return redisTemplate.getExpire(key);
    }

    public boolean setIfAbsent(String key, Object value) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value);
        } catch (Exception e) {
            log.error("Failed to setIfAbsent");
            return false;
        }
    }

    public boolean setIfAbsent(String key, Object value, long expireTime, TimeUnit timeUnit) {
        return (Boolean) redisTemplate.execute(new SessionCallback<Boolean>() {
            @SuppressWarnings("unchecked")
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                if (!setIfAbsent(key, value)) {
                    return false;
                }
                if (!expire(key, expireTime, timeUnit)) {
                    return false;
                }
                List<Object> exec = operations.exec();
                if (exec.size() > 0) {
                    return (Boolean) exec.get(0);
                }
                return false;
            }
        });
    }

    public boolean lock(String key, long expireTime) {
        //uuid：用来保证集群下，解锁只能由上锁人执行
        String uuid = UUID.randomUUID().toString();
        threadLocal.set(uuid);
        return setIfAbsent(key, uuid, expireTime, TimeUnit.SECONDS);
    }

    public void unLock(String key) {
        String uuid = threadLocal.get();
        if(uuid == null){
            log.error("Failed to UnLock, uuid id null , key:[{}]",key);
        }else {
            try {
               if(uuid.equals(get(key))){
                   threadLocal.remove();//防止内存溢出
                   //删除操作
                   remove(key);
               }else {
                   log.error("Failed to UnLock, uuid id not equals, key:[{}]",key);
               }
            }catch (Exception e){
                log.error("Failed to UnLock",e);
            }
        }
    }


}