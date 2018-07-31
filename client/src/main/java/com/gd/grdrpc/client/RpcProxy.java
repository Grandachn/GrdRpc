package com.gd.grdrpc.client;


import com.gd.grdrpc.bean.RpcRequest;
import com.gd.grdrpc.bean.RpcResponse;
import com.gd.grdrpc.discovery.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;
import java.util.UUID;

@Component
public class RpcProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcProxy.class);

    private String serverAddress;

    @Autowired
    private ServiceDiscovery serviceDiscovery;

//    public RpcProxy(String serverAddress) {
//        this.serverAddress = serverAddress;
//    }
//
//    public RpcProxy(ServiceDiscovery serviceDiscovery) {
//        this.serviceDiscovery = serviceDiscovery;
//    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<?> interfaceClass) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                (proxy, method, args) -> {
                    RpcRequest request = new RpcRequest(); // 创建并初始化 RPC 请求
                    request.setRequestId(UUID.randomUUID().toString());
                    request.setClassName(method.getDeclaringClass().getName());
                    request.setMethodName(method.getName());
                    request.setParameterTypes(method.getParameterTypes());
                    request.setParameters(args);

                    if (serviceDiscovery != null) {
                        serverAddress = serviceDiscovery.discover(); // 发现服务
                    }

                    String[] array = serverAddress.split(":");
                    String host = array[0];
                    int port = Integer.parseInt(array[1]);
                    RpcClient client = new RpcClient(host, port); // 初始化 RPC 客户端
                    LOGGER.info("request:" + request.getClassName()+ "----" + request.getMethodName());
                    RpcResponse response = client.send(request); // 通过 RPC 客户端发送 RPC 请求并获取 RPC 响应
                    LOGGER.info(response.toString());
//                    if (response.isError()) {
//                        throw response.getError();
//                    } else {
                    return response.getResult();
//                    }
                }
        );
    }
}