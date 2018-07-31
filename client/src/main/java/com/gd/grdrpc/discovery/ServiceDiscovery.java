package com.gd.grdrpc.discovery;

import com.gd.grdrpc.constant.Constant;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private Map<String, List<String>> serverMap = new HashMap<>();

    private Map<String, Integer> useCount = new ConcurrentHashMap<>();

    public ServiceDiscovery(@Value(("${zookeeper.address}")) String registryAddress) {
        ZooKeeper zk = connectServer(registryAddress);
        if (zk != null) {
            watchNode(zk);
        }
    }


    public String discover(String serveiceName) {
        String data = "";
        List<String> ipList = serverMap.get(serveiceName);
        int size = serverMap.get(serveiceName).size();
        if (size > 0) {
            if (size == 1) {
                data = ipList.get(0);
                LOGGER.info("using only server: {}", data);
            } else {

                //todo 负载均衡策略的加载
//                data = ipList.get(ThreadLocalRandom.current().nextInt(size));
//                LOGGER.info("using random server: {}", data);

                if (! useCount.containsKey(serveiceName)){
                    useCount.put(serveiceName, 0);
                }
                data = ipList.get(useCount.get(serveiceName) % ipList.size() );
                useCount.put(serveiceName, useCount.get(serveiceName) + 1);
                LOGGER.info("using lunxun server: {}", data);
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
                    serverMap.put(serviceKey, new ArrayList<>());
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
