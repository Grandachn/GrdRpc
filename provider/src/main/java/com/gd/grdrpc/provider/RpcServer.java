package com.gd.grdrpc.provider;


import com.gd.grdrpc.annotation.GrdRpcService;
import com.gd.grdrpc.bean.RpcRequest;
import com.gd.grdrpc.bean.RpcResponse;
import com.gd.grdrpc.coder.RpcDecoder;
import com.gd.grdrpc.coder.RpcEncoder;
import com.gd.grdrpc.handler.RpcHandler;
import com.gd.grdrpc.registry.ServiceRegistry;
import com.gd.grdrpc.utils.NetworkUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Administrator granda
 */
@Component
public class RpcServer implements ApplicationContextAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    @Value("${provider.port}")
    private int port;

    @Resource
    private ServiceRegistry serviceRegistry;

    /**
     * 存放接口名与服务对象之间的映射关系
     */
    private Map<String, Object> handlerMap = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        // 获取所有带有 GrdRpcService 注解的 Spring Bean
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(GrdRpcService.class);
        if (!serviceBeanMap.isEmpty()) {
            for (Object serviceBean : serviceBeanMap.values()) {
                String interfaceName = serviceBean.getClass().getAnnotation(GrdRpcService.class).value().getName();
                handlerMap.put(interfaceName, serviceBean);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    // 将 RPC 请求进行解码（为了处理请求）
                                    .addLast(new RpcDecoder(RpcRequest.class))
                                    // 将 RPC 响应进行编码（为了返回响应）
                                    .addLast(new RpcEncoder(RpcResponse.class))
                                    // 处理 RPC 请求
                                    .addLast(new RpcHandler(handlerMap));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(NetworkUtil.getLocalHostLANAddress().getHostAddress(), port).sync();
            LOGGER.info("server started on {}，{}",NetworkUtil.getLocalHostLANAddress().getHostAddress() , port);

            // 注册服务地址
            if (serviceRegistry != null) {
                serviceRegistry.register(NetworkUtil.getLocalHostLANAddress().getHostAddress() + ":" + port);
                LOGGER.info("服务成功注册到zookeeper");
            }

            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}