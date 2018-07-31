package com.gd.grdrpc.service.impl;

import com.gd.grdrpc.annotation.GrdRpcService;
import com.gd.grdrpc.service.CalService;

@GrdRpcService(CalService.class)
public class CalServiceImpl implements CalService {
    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
