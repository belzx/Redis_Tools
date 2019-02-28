package com.lizhi.utils;

public interface RetryCallBack<T> {

    T doWithRetry();

    boolean isComplete(T result);
}
