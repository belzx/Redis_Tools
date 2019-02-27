package com.lizhi.service;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface IRedisBloomFilter<E> {

    void setRedisService(IRedisService redisService);

    void init(String key ,long expireTime, TimeUnit timeUnit);

    void init(String key);

    void add(String key, E element);

    void addAll(String key, Collection<? extends E> c);

    boolean contains(String key, E element);

    boolean containsAll(String key, Collection<? extends E> c);
}
