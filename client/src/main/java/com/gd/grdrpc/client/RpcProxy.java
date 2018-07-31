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

/**
 * @author Administrator granda
 */
@Component
public class RpcProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcProxy.class);

    private String serverAddress;

    @Autowired
    private ServiceDiscovery serviceDiscovery;

    @SuppressWarnings("unchecked")
    public <T> T create(Class<?> interfaceClass) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                (proxy, method, args) -> {
                    // 创建并初始化 RPC 请求
                    RpcRequest request = new RpcRequest();
                    request.setRequestId(UUID.randomUUID().toString());
                    request.setClassName(method.getDeclaringClass().getName());
                    request.setMethodName(method.getName());
                    request.setParameterTypes(method.getParameterTypes());
                    request.setParameters(args);

                    if (serviceDiscovery != null) {
                        // 发现服务
                        serverAddress = serviceDiscovery.discover();
                    }

                    String[] array = serverAddress.split(":");
                    String host = array[0];
                    int port = Integer.parseInt(array[1]);
                    // 初始化 RPC 客户端
                    RpcClient client = new RpcClient(host, port);
                    LOGGER.info("request:" + request.getClassName()+ "----" + request.getMethodName());
                    // 通过 RPC 客户端发送 RPC 请求并获取 RPC 响应
                    RpcResponse response = client.send(request);
                    if (null != response){
                        LOGGER.info(response.toString());
                        return response.getResult();
                    }else {
                        return new Integer(0);
                    }

//                    if (response.isError()) {
//                        throw response.getError();
//                    } else {

//                    }
                }
        );
    }
}