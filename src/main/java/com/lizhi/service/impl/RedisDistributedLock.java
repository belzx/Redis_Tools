package com.lizhi.service.impl;

import com.lizhi.service.AbstractDistributedLock;
import com.lizhi.utils.RetryCallBack;
import com.lizhi.utils.RetryTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author https://github.com/lizhixiong1994
 * @Date 2019-02-28
 * 上锁的时候，可能会遇到如下几个问题
 * <p>
 * 1. 在调用 setIfAbsent 方法之后线程挂掉了，即没有给锁定的资源设置过期时间，默认是永不过期
 * <p>
 * 解决：利用redis的原子操作，事务包裹，set同时设值失效时间，同成功或同失败
 * <p>
 * 2. 线程T1获取锁
 * <p>
 * 线程T1执行业务操作，由于某些原因阻塞了较长时间
 * <p>
 * 锁自动过期，即锁自动释放了
 * <p>
 * 线程T2获取锁
 * <p>
 * 线程T1业务操作完毕，释放锁（其实是释放的线程T2的锁）
 * <p>
 * 解决： threadLocal保存uuid，作为value传入，解锁时对比uuid值，基本可以保证加锁解锁都是同一人
 * <p>
 * 3. 集群的极端情况系下，会有如下问题
 * <p>
 * 线程T1获取锁成功
 * <p>
 * Redis 的master节点挂掉，slave自动顶上
 * <p>
 * 线程T2获取锁，会从slave节点上去判断锁是否存在，由于Redis的master slave复制是异步的，所以此时线程T2可能成功获取到锁
 * <p>
 * 解决：建议lock锁后，再次确认lock是不是自己的 isLock（key），比较耗性能。双重检查
 */
@Component
public class RedisDistributedLock extends AbstractDistributedLock {

    @Autowired
    private RedisService redisService;

    private static final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedLock.class);

    @Override
    public boolean lock(String key, long expire, int retryTimes, long sleepMillis) {
        final String uuid = UUID.randomUUID().toString();
        Boolean execute = new RetryTemplate(retryTimes, sleepMillis){
            @Override
            public <T> void callbackSucceeded(T result) {
                super.callbackSucceeded(result);
                threadLocal.set(uuid);//锁成功则set
            }
        }.execute(new RetryCallBack<Boolean>() {
            @Override
            public Boolean doWithRetry() {
                return redisService.setIfAbsent(key, uuid, expire, TimeUnit.MILLISECONDS);
            }

            @Override
            public boolean isComplete(Boolean result) {
                return result;
            }
        });

        if (execute == null || execute == false) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean isLock(String key) {
        String uuid = threadLocal.get();
        if (uuid != null && uuid.equals(redisService.get(key))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean releaseLock(String key) {
        try {
            // 释放锁的时候，有可能因为持锁之后方法执行时间大于锁的有效期，此时有可能已经被另外一个线程持有锁，所以不能直接删除
            // 首先判断锁的持有者是不是自己
            String uuid = threadLocal.get();
            if (uuid == null) {
                LOGGER.error("Failed to releaseLock ,because UIID is null , key:[{}]", key);
                return true;
            } else if (uuid.equals(redisService.get(key))) {
                threadLocal.remove();//防止内存溢出
                redisService.remove(key); //删除操作
                return true;
            } else {
                LOGGER.error("Failed to releaseLock, lock not belong to oneself, key:[{}]", key);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to releaseLock", e);
            return false;
        }
    }
}
