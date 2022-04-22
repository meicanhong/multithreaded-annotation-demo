package com;


import java.time.Instant;

/**
 * @Author: canhong
 * @Date: 2022/4/22 10:45
 */
class DemoTest {

    @org.junit.jupiter.api.Test
    void test() throws Exception {
        long start, end;

        Demo demo = new Demo();
        start = Instant.now().toEpochMilli();
        demo.all();
        end = Instant.now().toEpochMilli();
        System.out.println(String.format("单线程耗时%d ms", end-start));

        Demo demo1 = new Demo();
        start = Instant.now().toEpochMilli();
        demo1.all1();
        end = Instant.now().toEpochMilli();
        System.out.println(String.format("多线程耗时%d ms", end-start));
    }

}