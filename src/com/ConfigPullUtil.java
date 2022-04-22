package com;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: canhong
 * @Date: 2022/4/22 10:29
 */
public class ConfigPullUtil {
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8, 60, TimeUnit.SECONDS, new ArrayBlockingQueue(100));

    /**
     * 配置采集-多线程
     * @param configClass 配置采集Class类
     * @param instance 配置采集类实例
     * @throws ClassNotFoundException
     */
    public static void configPullExecute(Class<? extends Object> configClass, Object instance) {
        List<Method> configMethods = getMethods(configClass);
        List<Future<Boolean>> futures = execute(configMethods, instance);
        checkFutures(futures);
    }

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
                public Boolean call() throws InvocationTargetException, IllegalAccessException {
                    method.invoke(instance);
                    return true;
                }
            };
            futures.add(threadPoolExecutor.submit(callable));
        }
        return futures;
    }

    /**
     * 检查配置采集结果，有异常则抛出
     * @param futures
     */
    private static void checkFutures(List<Future<Boolean>> futures) {
        try {
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
