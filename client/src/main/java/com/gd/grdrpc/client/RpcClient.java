package com.gd.grdrpc.client;


import com.gd.grdrpc.bean.RpcRequest;
import com.gd.grdrpc.bean.RpcResponse;
import com.gd.grdrpc.coder.RpcDecoder;
import com.gd.grdrpc.coder.RpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.*;


public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

    @Value("${timeout}")
    private Long timeout;

    final ExecutorService exec = Executors.newFixedThreadPool(1);

    private String host;
    private int port;

    private RpcResponse response;

    private RpcRequest request;

    private final Object obj = new Object();

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        this.response = response;

        synchronized (obj) {
            obj.notifyAll(); // 收到响应，唤醒线程
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("client caught exception", cause);
        ctx.close();
    }

    public RpcResponse send(RpcRequest request) {
        this.request = request;
        try {
            Future<String> future1 = exec.submit(call);
            //任务处理超时时间设为 1 秒
            String obj = future1.get(1000, TimeUnit.MILLISECONDS);

            return response;
        } catch (TimeoutException e){
            LOGGER.info("远程调用超时");
        }catch (InterruptedException | ExecutionException e){
            LOGGER.info("远程调用出錯");
        }
        return null;
    }

    private Callable<String> call = new Callable<String>() {
        @Override
        public String call() throws Exception {
            //开始执行耗时操作
            rpcSend(request);
            return "线程执行完成.";
        }
    };


    private void rpcSend(RpcRequest request) throws Exception{
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    // 将 RPC 请求进行编码（为了发送请求）
                                    .addLast(new RpcEncoder(RpcRequest.class))
                                    // 将 RPC 响应进行解码（为了处理响应）
                                    .addLast(new RpcDecoder(RpcResponse.class))
                                    // 使用 RpcClient 发送 RPC 请求
                                    .addLast(RpcClient.this);
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.connect(host, port).sync();
            future.channel().writeAndFlush(request).sync();

            synchronized (obj) {
                obj.wait(); //挂起
            }

            if (response != null) {
                future.channel().closeFuture().sync();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            group.shutdownGracefully();
        }
    }
}