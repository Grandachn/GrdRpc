package com.gd.grdrpc.loadbalance;

import java.util.List;
import java.util.Map;

/**
 * @Author by guanda
 * @Date 2018/8/1 20:54
 */
public class PollLoadBalance implements LoadBalanceType{

    @Override
    public String choiceHost(List<String> ipList, Map<String, Integer> useCount, String serveiceName) {
        if (! useCount.containsKey(serveiceName)){
            useCount.put(serveiceName, 0);
        }
        useCount.put(serveiceName, useCount.get(serveiceName) + 1);
        return ipList.get(useCount.get(serveiceName) % ipList.size());
    }

}
