package com.lizhi.utils;

/**
 * @author https://github.com/lizhixiong1994
 * @Date 2019-02-28
 */
public interface RetryCallBack<T> {

    T doWithRetry();

    boolean isComplete(T result);
}
