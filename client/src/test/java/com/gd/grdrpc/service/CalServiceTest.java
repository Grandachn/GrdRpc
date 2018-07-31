package com.gd.grdrpc.service;

import com.gd.grdrpc.client.RpcProxy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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

        CalService calService = rpcProxy.create(CalService.class);
        int result = calService.add(1,2);
        assertEquals(3 , result);
    }

}