package com.gd.grdrpc.loadbalance;

import java.util.List;
import java.util.Map;

/**
 * @Author by guanda
 * @Date 2018/8/1 20:49
 */
public interface LoadBalanceType {
    String choiceHost (List<String> ipList, Map<String, Integer> useCount, String serveiceName);
}
