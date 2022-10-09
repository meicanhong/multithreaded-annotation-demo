## 背景
目前我们API的配置采集是单线程执行配置采集，只有当一个Http请求结束时才能执行下一个Http请求。
Http1 -> Http2 -> Http3 -> ... -> Http4
整个过程是串行的
配置采集主要是进行大量的Http请求去拉取设备配置，线程在发起Http请求后会进入等待状态，直到Http返回结果线程才继续运行。整改配置采集的过程中很少会有依赖关系，每个Http请求都可以独立完成。
![image.png](https://cdn.nlark.com/yuque/0/2022/png/21385292/1648884107448-9c655cf0-b3f5-43d9-9898-cb5f27f16176.png#clientId=ueb4cc323-9c40-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=129&id=u00d3c2de&margin=%5Bobject%20Object%5D&name=image.png&originHeight=193&originWidth=1172&originalType=binary&ratio=1&rotation=0&showTitle=false&size=24867&status=done&style=none&taskId=uc7480ebd-2bdb-485a-9afc-ba0e38abd18&title=&width=781.3333333333334)
所以我们不如使用多线程的技术，使用多个线程去发送Http请求，每个线程分别完成一种资源的配置拉取。这一来，就不需要堵塞等待其他无关资源的http请求返回了。
![image.png](https://cdn.nlark.com/yuque/0/2022/png/21385292/1648884543042-eca6350c-9f63-4430-8d13-dc439d9ebb95.png#clientId=ueb4cc323-9c40-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=358&id=u2d0dfcc6&margin=%5Bobject%20Object%5D&name=image.png&originHeight=537&originWidth=963&originalType=binary&ratio=1&rotation=0&showTitle=false&size=57177&status=done&style=none&taskId=udb1c2a49-2877-406a-bcf3-14a48f88d7f&title=&width=642)

基于以上想法，我实现了在方法体上添加注解，即可将该方法放入线程池中运行的一种功能。
## 添加注解开启多线程
_注意，该注解目前只能应用与无参函数上。_
### 如何使用

1. 在需要开启多线程的方法上添加注解@ConfigPull
```java
@ConfigPull
public void getNodes() throws ConfigurationCollectionException {
    StringBuilder nodesUrl = new StringBuilder(getUrlPrefix() + ApiSuffix.F5_NODE.getValue());
    JSONArray nodes = getObjJsonArray(nodesUrl);
    JSONObject nodesJson = JSONUtil.createObj().putOnce(ApiDeviceCommonVar.NODES.getValue(), nodes);

    // 获取Node的状态信息
    StringBuilder nodeStatsUrl = new StringBuilder(getUrlPrefix() + ApiSuffix.F5_NODE_STATS.getValue());
    HashMap<String, String> headers = getPredefineHeader();
    JSONObject allNodeStats =
            sendHttpdRequest(nodeStatsUrl.toString(), ApiDeviceCommonVar.GET.getValue(), headers, null);

    nodesJson.putOnce(ApiDeviceCommonVar.NODES_STATS.getValue(), allNodeStats);

    this.config.put(ApiDeviceCommonVar.NODES.getValue(), nodesJson);
}
```

2. 在配置采集类的getAllConfig方法中调用执行多线程配置采集的方法
```java
public void getAllConfig2() throws ConfigurationCollectionException {
     /**
      * 启动多线程配置采集
      */
     ConfigPullUtil.configPullExecute(F5LoadBalance.class, this);
}
```

### 多线程配置采集是如何执行的呢？
```java
/**
 * 配置采集-多线程
 * @param configClass 配置采集Class类
 * @param instance 配置采集类实例
 * @throws ConfigurationCollectionException
 * @throws ClassNotFoundException
 */
public static void configPullExecute(Class<? extends Object> configClass, Object instance) throws ConfigurationCollectionException {
    List<Method> configMethods = getMethods(configClass);
    List<Future<Boolean>> futures = execute(configMethods, instance);
    checkFutures(futures);
}
```
主要分为三步：

1. 获取添加@ConfigPull注解的方法
2. 通过反射重写Callbale的call方法，注入配置拉取的方法。
3. 遍历所有任务的结果集，主要是查找配置拉取的时候有没有发生异常，有异常则抛出。

![image.png](https://cdn.nlark.com/yuque/0/2022/png/21385292/1648919664685-6972ba6f-f996-4a86-b25d-f14030581e27.png#clientId=ubd30f913-816f-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=ut5jY&margin=%5Bobject%20Object%5D&name=image.png&originHeight=924&originWidth=722&originalType=binary&ratio=1&rotation=0&showTitle=false&size=82465&status=done&style=none&taskId=udbc8d3ea-e614-415e-a59d-b829fb97521&title=)

#### 获取被注解标记的方法
通过反射获取该类的所有方法，判断方法上是否存在@ConfigPull注解
若存在注解，则添加进队列。
```java
/**
 * 获取类上被注解标识的方法
 * @param configClass
 * @return
 */
private static List<Method> getMethods(Class<? extends Object> configClass) {
    List<Method> configMethods = new ArrayList<>();
    Method[] methods = configClass.getMethods();
    for (Method method : methods) {
        if (method.isAnnotationPresent(ConfigPull.class)) {
            configMethods.add(method);
        }
    }
    return configMethods;
}
```

#### 构造Callable实例并放入线程池执行，并等待所有任务执行完成
遍历前面得到的包含所有被注解标识的方法队列
构建Callable实例，重写call()方法，主要是调用当前method
将Callable实例添加到线程池中执行，并将执行结果添加至Futures队列中，以备后续检查
```java
/**
 * 构造Callable实例并放入线程池执行
 * 返回线程执行结果
 * @param configMethods
 * @param instance
 */
private static List<Future<Boolean>> execute(List<Method> configMethods, Object instance) {
    List<Future<Boolean>> futures = new ArrayList<>();
    for (Method method : configMethods) {
        Callable<Boolean> callable = new Callable<Boolean>() {
            @Override
            public Boolean call() throws ConfigurationCollectionException {
                try {
                    method.invoke(instance);
                    return true;
                } catch (InvocationTargetException | IllegalAccessException e) {
                    ConfigurationCollectionException configurationCollectionException = new ConfigurationCollectionException(e.getMessage());
                    configurationCollectionException.initCause(e);
                    throw configurationCollectionException;
                }
            }
        };
        futures.add(configPullExecutor.submit(callable));
    }
    return futures;
}
```

#### 检查配置采集结果，有异常则抛出
遍历Futures队列，检查结果。
若遇到异常则抛出
若无异常，则程序执行完毕
```java

/**
 * 检查配置采集结果，有异常则抛出
 * @param futures
 * @throws ConfigurationCollectionException
 */
private static void checkFutures(List<Future<Boolean>> futures) throws ConfigurationCollectionException {
    try {
        for (Future<Boolean> future : futures) {
            future.get();
        }
    } catch (ExecutionException e) {
        logger.error("配置采集执行出错", e);
        ConfigurationCollectionException configurationCollectionException = new ConfigurationCollectionException(e.getMessage());
        configurationCollectionException.initCause(e);
        throw configurationCollectionException;
    } catch (InterruptedException e) {
        logger.error("配置采集执行中断", e);
        ConfigurationCollectionException configurationCollectionException = new ConfigurationCollectionException(e.getMessage());
        configurationCollectionException.initCause(e);
        Thread.currentThread().interrupt();
        throw configurationCollectionException;
    }
}
```

### 测试对比速度和数据是否准确
```java
@Test(dataProvider = "getAbstractApiLoadBalanceDevices")
    public void getAllConfigTest(F5LoadBalance abstractApiLoadBalanceDevice)
            throws ConfigurationCollectionException, ClassNotFoundException {
        long start, end;
        F5LoadBalance f5LoadBalance = new F5LoadBalance(grpcClient, "https", "192.168.1.233", "443", "admin", "xiongmin", "Common",
                F5Consts.V15.getValue());
        for (int i = 0; i < 10; i++) {
            start = Instant.now().toEpochMilli();
            String allConfigString = abstractApiLoadBalanceDevice.getAllConfigString();
            end = Instant.now().toEpochMilli();
            System.out.println(String.format("单线程，耗时: %d ms", end - start));

            start = Instant.now().toEpochMilli();
            f5LoadBalance.getAllConfig2();
            String s = JSONUtil.toJsonStr(f5LoadBalance.getConfig());
            end = Instant.now().toEpochMilli();
            System.err.println(String.format("使用多线程，耗时: %d ms", end - start));
            System.out.println("单线程数据长度：" + allConfigString.length() + "  多线程数据长度：" + s.length());
        }
    }
```
![image.png](https://cdn.nlark.com/yuque/0/2022/png/21385292/1649240895315-f46f08a0-b7c7-4e73-b94f-2ebe08656133.png#clientId=u25b7222f-bcb7-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=u61569ba2&margin=%5Bobject%20Object%5D&name=image.png&originHeight=478&originWidth=450&originalType=binary&ratio=1&rotation=0&showTitle=false&size=42537&status=done&style=none&taskId=u471e6ab9-74c0-4306-aaf1-59e5e9dbc0f&title=)

### 检测异常是否能正常抛出
![image.png](https://cdn.nlark.com/yuque/0/2022/png/21385292/1649238712437-99ab37f4-320c-4650-b38e-6c2b5b628cd2.png#clientId=u25b7222f-bcb7-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=350&id=u12aec9d2&margin=%5Bobject%20Object%5D&name=image.png&originHeight=525&originWidth=1220&originalType=binary&ratio=1&rotation=0&showTitle=false&size=132717&status=done&style=none&taskId=u253212c9-d83f-4523-b18c-b3dab20fbdc&title=&width=813.3333333333334)
