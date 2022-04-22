package com;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: canhong
 * @Date: 2022/4/22 10:21
 */
public class Demo {
    private ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    public void all() throws Exception {
        aaa();
        bbb();
        ccc();
        ddd();
        print();
    }

    public void all1() {
        ConfigPullUtil.configPullExecute(Demo.class, this);
        print();
    }

    @ConfigPull
    public void aaa() throws Exception {
        Thread currentThread = Thread.currentThread();
        System.out.println(String.format("aaa %s 开始", currentThread.getName()));
        try {
            Thread.sleep(1000L);
            map.put("a", "a");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(String.format("aaa %s 结束", currentThread.getName()));
//        throw new Exception("抛一个异常");
    }

    @ConfigPull
    public void bbb() {
        Thread currentThread = Thread.currentThread();
        System.out.println(String.format("bbb %s 开始", currentThread.getName()));
        try {
            Thread.sleep(1000L);
            map.put("b", "b");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(String.format("bbb %s 结束", currentThread.getName()));
    }

    @ConfigPull
    public void ccc() {
        Thread currentThread = Thread.currentThread();
        System.out.println(String.format("ccc %s 开始", currentThread.getName()));
        try {
            Thread.sleep(1000L);
            map.put("c", "c");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(String.format("ccc %s 结束", currentThread.getName()));
    }

    @ConfigPull
    public void ddd() {
        Thread currentThread = Thread.currentThread();
        System.out.println(String.format("ddd %s 开始", currentThread.getName()));
        try {
            Thread.sleep(1000L);
            map.put("d", "d");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(String.format("ddd %s 结束", currentThread.getName()));
    }

    public void print() {
        System.out.println("print");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.print(entry.getKey());
        }
        System.out.println("");
    }
}
