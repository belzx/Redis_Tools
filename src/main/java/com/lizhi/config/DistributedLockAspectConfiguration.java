//package com.lizhi.config;
//
//import com.lizhi.service.IDistributedLock;
//import com.lizhi.utils.RedisLock;
//import org.aspectj.lang.annotation.Aspect;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.util.StringUtils;
//
//import java.lang.reflect.Method;
//import java.util.Arrays;
//
///**
// * @version 1.0.0
// */
//@Aspect
//@Configuration
//@ConditionalOnClass(DistributedLock.class)
//@AutoConfigureAfter(DistributedLockAutoConfiguration.class)
//public class DistributedLockAspectConfiguration {
//
//    private final Logger logger = LoggerFactory.getLogger(DistributedLockAspectConfiguration.class);
//
//    @Autowired
//    private IDistributedLock distributedLock;
//
//    @Pointcut("@annotation(com.lizhi.utils.RedisLock)")
//    private void lockPoint(){
//
//    }
//
//    @Around("lockPoint()")
//    public Object around(ProceedingJoinPoint pjp) throws Throwable{
//        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
//        RedisLock redisLock = method.getAnnotation(RedisLock.class);
//        String key = redisLock.value();
//        if(StringUtils.isEmpty(key)){
//            Object[] args = pjp.getArgs();
//            key = Arrays.toString(args);
//        }
//        int retryTimes = redisLock.action().equals(RedisLock.LockFailAction.CONTINUE) ? redisLock.retryTimes() : 0;
//        boolean lock = distributedLock.lock(key, redisLock.keepMills(), retryTimes, redisLock.sleepMills());
//        if(!lock) {
//            logger.debug("get lock failed : " + key);
//            return null;
//        }
//
//        //得到锁,执行方法，释放锁
//        logger.debug("get lock success : " + key);
//        try {
//            return pjp.proceed();
//        } catch (Exception e) {
//            logger.error("execute locked method occured an exception", e);
//        } finally {
//            boolean releaseResult = distributedLock.releaseLock(key);
//            logger.debug("release lock : " + key + (releaseResult ? " success" : " failed"));
//        }
//        return null;
//    }
//}
