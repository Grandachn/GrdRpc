package com.gd.grdrpc.service;

import com.gd.grdrpc.client.RpcProxy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * @Author by guanda
 * @Date 2018/7/31 10:46
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class CalServiceTest {

    @Autowired
    private RpcProxy rpcProxy;


    @Test
    public void add() throws Exception {

        long start = System.currentTimeMillis();
        CalService calService = rpcProxy.create(CalService.class);
        for (int i = 0;i < 1000; i++){

            Object result = calService.add(1,2);
            if (result.equals(new Integer(0))){
                Thread.sleep(1000);
            }
//            if (result instanceof Integer){
//                assertEquals(new Integer(3) , result);
//            }

        }
        System.out.println("花费时间" + (System.currentTimeMillis() - start));
    }

    @Test
    public void addThreads() throws Exception {
        for (int i = 0; i < 10; i ++){
            Thread t = new MyThread("mythread--" + i);
            t.start();
        }

        Thread.sleep(100000);
    }

    class MyThread extends Thread{
        private String name;

        public MyThread(String name){
            this.name = name;
        }

        @Override
        public void run() {
            try {
                add();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}