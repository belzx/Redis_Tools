## redis工具包
## 功能：redis常用方法、redis分布式锁
## 1. redis常用方法
    参考IRedisService接口。
## 2. redis分布式锁

接口：
```
        /**
         * 
         * @param key keyname
         * @param expire 失效时间ms
         * @param retryTimes 重试次数
         * @param sleepMillis 重试间隔ms
         * @return 成功tru，失败false
         */
        @Override
        public boolean lock(String key, long expire, int retryTimes, long sleepMillis) ；
        
        @Override
         public void releaseLock(String key) ；
        
```

例子：
```
@Test
    public void testRedisLock() {
        try {
            new Thread(() -> {
                System.out.println("Thread1 try to get lock ...");
                if (redisDistributedLock.lock("11111")) {
                    System.out.println("Thread1 try success  get lock");
                    try {
                        System.out.println("Thread1 do somethins ...");
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Thread1 over");
                    } finally {
                        redisDistributedLock.releaseLock("11111");
                    }
                }
            }).start();

            Thread.sleep(1000);

            new Thread(() -> {
                System.out.println("Thread2 try to get lock ...");
                if (redisDistributedLock.lock("11111", 4, 3000L)) {
                    try {
                        System.out.println("Thread2 do somethins ...");
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Thread2 over");
                    } finally {
                        redisDistributedLock.releaseLock("11111");
                    }
                } else {
                    System.out.println("Thread2 Failed to lock");
                }
            }).start();

            Thread.sleep(2222222);

        } catch (Exception e) {
            log.error("", e);
        }
    }
```

结果：

```
Thread1 try to get lock ...
Thread1 try success  get lock
Thread1 do somethins ...
Thread2 try to get lock ...
Thread1 over
2019-03-03 22:47:18.658 [Thread-6] WARN  com.lizhi.utils.RetryTemplate - com.lizhi.service.impl.RedisDistributedLock$1retry:[1]times,cast [3001]ms
Thread2 do somethins ...
Thread2 over
```

AOP注解实现上锁：

```
    @RedisLock(value = "123")//value尽可能复杂一点，避免重复
    public void tt() {
        System.out.println("testttt2");
    }
    
```

上锁的时候，可能会遇到如下几个问题

1. 在调用 setIfAbsent 方法之后线程挂掉了，即没有给锁定的资源设置过期时间，默认是永不过期

解决：利用redis的原子操作，事务包裹，set同时设值失效时间，同成功或同失败

2. 线程T1获取锁

    线程T1执行业务操作，由于某些原因阻塞了较长时间
    
    锁自动过期，即锁自动释放了
    
    线程T2获取锁
    
    线程T1业务操作完毕，释放锁（其实是释放的线程T2的锁）
    
解决： threadLocal保存uuid，作为value传入，解锁时对比uuid值，基本可以保证加锁解锁都是同一人

3. 集群的极端情况系下，会有如下问题

    线程T1获取锁成功
    
    Redis 的master节点挂掉，slave自动顶上
    
    线程T2获取锁，会从slave节点上去判断锁是否存在，由于Redis的master slave复制是异步的，所以此时线程T2可能成功获取到锁
    
解决：lock锁后，再次确认lock是不是自己的 isLock（key），双重检查

## 3. 布隆过滤 

参照 https://blog.csdn.net/qq_18495465/article/details/78500472


```
        /**
         * @param falsePositiveProbability 容错率 默认0.0001
         * @param expectedNumberOfElements 容量   默认600000
         * @return
         */
        IRedisBloomFilter getBloomFilter(double falsePositiveProbability, int expectedNumberOfElements);
    
        IRedisBloomFilter getBloomFilter();
        
```
### 4.pub/sub
接口：
```
    public void sendMessage(String channel, String message) 
```

### 5.管道
接口：
```
    <T> T pipeline(PipelineTemplete<T> pipelineTemplete) 
```
例子：
```
  @Test
    public void testPipeline() {

        long a = System.currentTimeMillis();
        redisService.pipeline(new PipelineTemplete() {
            @Override
            public void pipelineExecute() {
                for (int i = 0; i < 100000; i++) {
                   redisService.set("123222" + i,2);
                }
            }

            @Override
            public void resultProcess(List<Object> list) {
                System.out.println(list);
            }
        });

        long b = System.currentTimeMillis();
        System.out.println("Pipeline cast:" + (b - a)+"ms");

        for (int i = 0; i < 100000; i++) {
            redisService.set("123222" + i, 2);
        }

        long c = System.currentTimeMillis();
        System.out.println("no pipeline cast:" + (c - b)+"ms");
    }
```
结果显而易见：
```
Pipeline cast:692ms
no pipeline cast:6509ms
```

### 思考： 
#### 1. 如何实现日统计在线人数，周统计在线人数
解决：

可以利用setBit实现。

假设登录人的id分别为 id=3 ，name=张三 ： id=4 ，name=李四 ：id=5 ，name=王五 ：

现2019-02-28 这三人都有登录：
    
    则：redisService.setBit("2019-02-28",3,true) //张三登录
    则：redisService.setBit("2019-02-28",4,true) //李四登录
    则：redisService.setBit("2019-02-28",5,true) //王五登录
    
2019-02-28统计：
    bit数组中，为true的下标一共有三个。则当日统计人数3人。
    
现2019-02-29 这二人都有登录：
    
    则：redisService.setBit("2019-02-29",3,true) //张三登录
    则：redisService.setBit("2019-02-29",5,true) //王五登录
    
2019-02-29 统计：
    bit数组中，为true的下标一共有2个。则当日统计人数2人。
    连续登录两天的人数 为两个bit的交集个数，2人。