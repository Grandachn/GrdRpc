package com.gd.grdrpc.discovery;

import com.gd.grdrpc.constant.Constant;
import com.gd.grdrpc.loadbalance.LoadBalanceType;
import com.gd.grdrpc.loadbalance.RandomLoadBalance;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;


/**
 * @Author by guanda
 * @Date 2018/7/31 11:11
 */
@Component
public class ServiceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscovery.class);

    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<String> dataList = new ArrayList<>();

    private Map<String, Set<String>> serverMap = new HashMap<>();

    private Map<String, Integer> useCount = new ConcurrentHashMap<>();

    private LoadBalanceType loadBalanceType;

        public ServiceDiscovery(@Value(("${zookeeper.address}")) String registryAddress,
                                @Value(("${comsumer.load-balance-type}"))String loadBalanceType) {
            ZooKeeper zk = connectServer(registryAddress);
            if (zk != null) {
                watchNode(zk);
            }

            try {
                this.loadBalanceType = (LoadBalanceType) Class.forName("com.gd.grdrpc.loadbalance." + loadBalanceType + "LoadBalance").newInstance();
                LOGGER.info("comsumer.load-balance-type use : [" + loadBalanceType + "]");
            } catch (Exception e) {
                LOGGER.error("comsumer.load-balance-type : [" + loadBalanceType + "] is not supported");
                LOGGER.info("comsumer.load-balance-type use [Random] instead");
            }finally {
                //默认使用随机算法
                this.loadBalanceType = new RandomLoadBalance();
            }
        }


    public String discover(String serveiceName) {
        String data = "";
        List<String> ipList = new ArrayList<>();
        ipList.addAll(serverMap.get(serveiceName));
        int size = ipList.size();
        if (size > 0) {
            if (size == 1) {
                data = ipList.get(0);
                LOGGER.info("using only server: {}", data);
            } else {
                // 负载均衡策略的加载
                data = loadBalanceType.choiceHost(ipList, useCount, serveiceName);
                LOGGER.info("using  server: {} ，count：{}", data);
            }
        }
        return data;
    }

    private ZooKeeper connectServer(String registryAddress) {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    latch.countDown();
                }
            });
            LOGGER.info("正在连接zookeeper...");
            latch.await();
            LOGGER.info("连接zookeeper成功");
        } catch (IOException | InterruptedException e) {
            LOGGER.error("连接zookeeper失败", e);
        }
        return zk;
    }

    private void watchNode(final ZooKeeper zk) {
        try {
            List<String> nodeList = zk.getChildren(Constant.ZK_REGISTRY_PATH, event -> {
                if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    watchNode(zk);
                }
            });
//            List<String> dataList = new ArrayList<>();

            for (String node : nodeList) {
                LOGGER.info("node : {}", node);
                byte[] bytes = zk.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false, null);
                String data = new String(bytes);
                String serviceKey = data.split("&")[1];
                String serviceIp = data.split("&")[2];
                if (!serverMap.containsKey(serviceKey)){
                    serverMap.put(serviceKey, new TreeSet<>());
                }
                serverMap.get(serviceKey).add(serviceIp);
//                dataList.add(new String(bytes));
            }
            LOGGER.info("node data: {}", dataList);
            this.dataList = dataList;
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error("", e);
        }
    }
}
