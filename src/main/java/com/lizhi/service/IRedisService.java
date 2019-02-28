package com.lizhi.service;

import com.lizhi.utils.PipelineTemplete;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface IRedisService {

    /**
     * 批量删除对应的value
     * @param keys
     */
    void remove(final String... keys);

    /**
     * 批量删除key
     * @param pattern
     */
    void removePattern(final String pattern);

    boolean expire(String key, Long expireTime, TimeUnit timeUnit);

    Long getExpireTime(String key);

    /**
     * 删除对应的value
     *
     * @param key
     */
    void remove(final String key);

    /**
     * 判断缓存中是否有对应的value
     *
     * @param key
     * @return
     */
    boolean exists(final String key);

    /**
     * 读取缓存
     *
     * @param key
     * @return
     */
    Object get(final String key);

    /**
     * 写入缓存
     * @param key
     * @param value
     * @return
     */
    boolean set(final String key, Object value);

    /**
     * 写入缓存，原子操作
     * @param key
     * @param value
     * @param expireTime 单位s
     * @return
     */
    boolean set(final String key, Object value, Long expireTime);

    boolean hmset(String key, Map<String, String> value);

    Map<String, String> hmget(String key);

    void lset(String key, Object object);

    Object lget(String key);

    Object lgetAll(String key);

    /**
     * 向某一个频道发送消息
     * “发布/订阅
     * @param channel
     * @param message
     */
    void sendMessage(String channel, String message);

    /**
     * setBit Boolean setBit(K key, long offset, boolean value);
     * 对 key 所储存的字符串值，设置或清除指定偏移量上的位(bit)
     * key键对应的值value对应的ascii码,在offset的位置(从左向右数)变为value
     * 因为二进制只有0和1，在setbit中true为1，false为0，因此我要变为'b'的话第六位设置为1，第七位设置为0
     *
     * @param key
     * @param index
     */
    boolean setBit(String key, long index, boolean value);

    boolean setBit(final String key, final long index, final boolean value, final long expireTime, final TimeUnit timeUnit);

    boolean getBit(String key, long index);

    /**
     * @param falsePositiveProbability 容错率 默认0.0001
     * @param expectedNumberOfElements 容量   默认600000
     * @return
     */
    IRedisBloomFilter getBloomFilter(double falsePositiveProbability, int expectedNumberOfElements);

    IRedisBloomFilter getBloomFilter();

    /**
     * 管道
     */
    <T> T pipeline(PipelineTemplete<T> pipelineTemplete);

}
