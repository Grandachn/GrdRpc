基本的实现思路：
1. 服务提供方向zookeeper注册服务，生成子节点，子节点包含接口名、服务ip、端口等信息的数据
2. 客户端发起接口的方法调用，使用jdk动态代理调用，线程挂起等待数据返回，等待超时结束线程，报远程调用错误
3. 将调用参数、方法名、接口名用protostuff 协议处理成二进制
4. 到zookeeper中获取注册的服务地址，并监控节子节点的变化（服务的注册、下线）
5. 借助netty框架（NIO）把调用数据传送给服务提供方
6. 服务端解析protostuff 消息，获取调用参数、方法名、接口名，找到接口对应的bean，通过反射调用方法，将结果包装成protostuff 消息，传回客户端
7. 客户端解析消息返回结果

服务提供者示例：
CalService.java
```
public interface CalService {
    int add(int a, int b);
}

```

CalServiceImpl.java
```
@GrdRpcService(CalService.class)
public class CalServiceImpl implements CalService {
    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
```
application.yml
```
server:
  port: 8096
  servlet:
    path:

provider:
  host: 127.0.0.1
  port: 8896
  name: gd-Cal

zookeeper:
  address: 127.0.0.1:2181
```

服务消费者示例：
application.yml
```
server:
  port: 8099

zookeeper:
  address: 127.0.0.1:2181

comsumer:
  load-balance-type: Poll

```
调用远程服务的java文件
```
    @Autowired
    private RpcProxy rpcProxy;

    public void add() throws Exception {
        //这里可以生成一个bean托管到Spring, 就没有必要每次都生成了
        CalService calService = rpcProxy.create(CalService.class);
        for (int i = 0;i < 10; i++){
            Object result = calService.add(1,2);
            if (result instanceof Integer){
                assertEquals(new Integer(3) , result);
            }
        }
    }
```
