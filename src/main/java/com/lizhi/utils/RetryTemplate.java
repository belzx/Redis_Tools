package com.lizhi.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryTemplate {

    private static final int DEFAULT_RETRY_TIMES = 3;

    private static final int DEFAULT_WAIT_TIME = 1000;

    private long waitTime = DEFAULT_WAIT_TIME;

    private int retryTimes = DEFAULT_RETRY_TIMES;

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryTemplate.class);

    public RetryTemplate() {
    }

    public RetryTemplate(long waitTime) {
        this.waitTime = waitTime;
    }

    public RetryTemplate(int retryTimes ,long waitTime) {
        this.retryTimes = retryTimes;
        this.waitTime = waitTime;
    }

    /**
     * 开始执行重试操作
     * @param callBack
     * @param <T>
     * @return null表示失败
     */
    public <T> T execute(RetryCallBack<T> callBack) {
        int executeRetryTimes = 0;
        int defaultRetryTimes = this.retryTimes;
        final long waitTime = this.waitTime;
        long start = 0;
        long stop = 0;
        long waitLeft;
        T result;
        do {
            executeRetryTimes++;
            start = System.currentTimeMillis();
            result = callBack.doWithRetry();
            if (callBack.isComplete(result)) {
                callbackSucceeded(stop);
                return result;
            } else {
                stop = System.currentTimeMillis();
                if (waitTime > 0) {
                    waitLeft = waitTime - (stop - start);
                    if (waitLeft > 0) {
                        try {
                            Thread.sleep(waitLeft);
                        } catch (InterruptedException e) {
                            stop = System.currentTimeMillis();
                            callBackFailed(stop);
                            throw new RuntimeException("Retry failed interrupted while waiting", e);
                        }
                    }else {

                    }
                }
                LOGGER.warn(callBack.getClass().getName() + "retry:[{}]times" + executeRetryTimes);
            }
        } while (executeRetryTimes < defaultRetryTimes);
        callbackFailed(stop);
        return null;
    }

    private void callBackFailed(long stop) {

    }

    private void callbackFailed(long stop) {
    }

    private void callbackSucceeded(long stop) {
    }

}
