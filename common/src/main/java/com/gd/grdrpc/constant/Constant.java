package com.gd.grdrpc.constant;

/**
 * @Author by guanda
 * @Date 2018/7/31 11:13
 */
public interface Constant {

    int ZK_SESSION_TIMEOUT = 50000;

    String ZK_REGISTRY_PATH = "/registry";
    String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";
}