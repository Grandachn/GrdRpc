package com.gd.grdrpc.loadbalance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Author by guanda
 * @Date 2018/8/1 20:51
 */
public class RandomLoadBalance implements LoadBalanceType {

    @Override
    public String choiceHost(List<String> ipList, Map<String, Integer> useCount, String serveiceName) {
        return ipList.get(ThreadLocalRandom.current().nextInt(ipList.size()));
    }

}
