package com.lizhi.service;

/**
 * @author https://github.com/lizhixiong1994
 * @Date 2019-02-28
 */
public interface IDistributedLock {

    long TIMEOUT_MILLIS = 30000;

    int RETRY_TIMES = Integer.MAX_VALUE;

    long SLEEP_MILLIS = 500;

    /**
     * @param key keyname
     * @return 成功：true 失败：false
     */
    boolean lock(String key);

    /**
     * @param key        keyname
     * @param retryTimes 重试次数
     * @return 成功：true 失败：false
     */
    boolean lock(String key, int retryTimes);

    /**
     * @param key         keyname
     * @param retryTimes  重试次数
     * @param sleepMillis 重试间隔ms
     * @return 成功：true 失败：false
     */
    boolean lock(String key, int retryTimes, long sleepMillis);

    /**
     * @param key    keyname
     * @param expire 失效时间ms
     * @return 成功：true 失败：false
     */
    boolean lock(String key, long expire);

    /**
     * @param key        keyname
     * @param expire     失效时间ms
     * @param retryTimes 重试次数
     * @return 成功：true 失败：false
     */
    boolean lock(String key, long expire, int retryTimes);

    /**
     * @param key         keyname
     * @param expire      失效时间ms
     * @param retryTimes  重试次数
     * @param sleepMillis 重试间隔ms
     * @return 成功：true 失败：false
     */
    boolean lock(String key, long expire, int retryTimes, long sleepMillis);

    /**
     * @param key         keyname
     */
    boolean releaseLock(String key);

    /**
     * @param key         keyname
     * @return true：锁在手上  false：锁不存在或不在自己手上
     */
    boolean isLock(String key);
}
