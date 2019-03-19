package com.lizhi.service;

import com.lizhi.utils.PipelineTemplete;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * redis 工具
 * 1：一些常用操作
 * 2：布隆过滤
 * 3：管道操作
 *
 * redisTemplate.opsForValue();//操作字符串
 * redisTemplate.opsForHash();//操作hash
 * redisTemplate.opsForList();//操作list
 * redisTemplate.opsForSet();//操作set
 * redisTemplate.opsForZSet();//操作有序set
 */
public interface IRedisService {

    /**
     * 批量删除对应的value
     *
     * @param keys
     */
    void remove(final String... keys);

    /**
     * 批量删除key
     *
     * @param keys 键
     */
    void removePattern(final String keys);


    /**
     * 删除对应的value
     *
     * @param key 键
     */
    void remove(final String key);

    /**
     * key是否存在
     *
     * @param key
     */
    boolean exists(final String key);

    /**
     * 获取value
     *
     * @param key 键
     * @return
     */
    Object get(final String key);

    /**
     * 设置key-value
     *
     * @param key
     * @param value
     * @return
     */
    boolean set(final String key, Object value);

    /**
     * 使用SessionCallBack这个接口，通过这个接口就可以把属于多个同一套命令放在同一个
     * Redis连接中去执行
     * 通过SessionCallBack 接口可以保证原子性
     * <p>
     * redis的事务是由multi和exec包围起来的部分，当发出multi命令时，redis会进入事务，redis会进入阻塞状态，不再响应任何别的客户端的请求，
     * 直到发出multi命令的客户端再发出exec命令为止。那么被multi和exec包围的命令会进入独享redis的过程，直到执行完毕
     *
     * @param key        键
     * @param value      值
     * @param expireTime 失效时间，单位毫秒
     * @return
     */
    boolean set(final String key, final Object value, final Long expireTime);

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    boolean hmset(String key, Map<String, String> map);

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    Map<String, String> hmget(String key);

    /**
     * 将list放入缓存
     *
     * @param key        键
     * @param value      值
     * @param expireTime 时间(秒)
     */
    boolean lset(String key, Object value, long expireTime, TimeUnit timeUnit);

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return
     */
    boolean lset(String key, Object value);

    /**
     * 获取list缓存的内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束  0 到 -1代表所有值
     * @return
     */
    List<Object> lget(String key, long start, long end);


    Object lgetAll(String key, long start, long end);

    /**
     * pub/sub
     * 发送信息
     *
     * @param channel 频道
     * @param message 信息
     * @return
     */
    void sendMessage(String channel, String message);

    /**
     * 获取BloomFilter工具
     *
     * @param falsePositiveProbability 容错率 默认0.0001
     * @param expectedNumberOfElements 容量   默认600000
     * @return
     */
    IRedisBloomFilter getBloomFilter(double falsePositiveProbability, int expectedNumberOfElements);

    /**
     * 获取BloomFilter工具
     *
     * @return
     */
    IRedisBloomFilter getBloomFilter();

    /**
     * setBit Boolean setBit(K key, long offset, boolean value);
     * 对 key 所储存的字符串值，设置或清除指定偏移量上的位(bit)
     * key键对应的值value对应的ascii码,在offset的位置(从左向右数)变为value
     * 因为二进制只有0和1，在setbit中true为1，false为0，因此我要变为'b'的话第六位设置为1，第七位设置为0
     *
     * @param key   键
     * @param index 下标
     * @param value false = 0，true = 1
     */
    boolean setBit(String key, long index, boolean value);

    /**
     * setBit Boolean setBit(K key, long offset, boolean value);
     * 对 key 所储存的字符串值，设置或清除指定偏移量上的位(bit)
     * key键对应的值value对应的ascii码,在offset的位置(从左向右数)变为value
     * 因为二进制只有0和1，在setbit中true为1，false为0，因此我要变为'b'的话第六位设置为1，第七位设置为0
     *
     * @param key        键
     * @param index      下标
     * @param value      false = 0，true = 1
     * @param expireTime 失效时间
     * @param timeUnit   失效单位
     */
    boolean setBit(final String key, final long index, final boolean value, final long expireTime, final TimeUnit timeUnit);

    /**
     * 位操作
     *
     * @param key   键
     * @param index 下标
     * @return
     */
    boolean getBit(String key, long index);

    /**
     * 失效时间
     *
     * @param key        键
     * @param expireTime 时长
     * @param timeUnit   单位
     * @return
     */
    boolean expire(String key, Long expireTime, TimeUnit timeUnit);

    /**
     * 获取失效时间
     *
     * @param key 键
     * @return 单位毫秒
     */

    Long getExpireTime(String key);

    /**
     * @param key   键
     * @param value 值
     * @return true：成功 false：失败
     */
    boolean setIfAbsent(String key, Object value);

    /**
     * @param key        键
     * @param value      值
     * @param expireTime 时间
     * @param timeUnit   单位
     * @return true：成功 false：失败
     */
    boolean setIfAbsent(String key, Object value, long expireTime, TimeUnit timeUnit);


    /**
     * 管道操作
     *
     * @param pipelineTemplete
     */
    List<Object>  pipeline(PipelineTemplete pipelineTemplete);

     boolean redisLock(final String key, String value, long expireTime, TimeUnit timeUnit);

     boolean releaseLock(String key, String value);
}
