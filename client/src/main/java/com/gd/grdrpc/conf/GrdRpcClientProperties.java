package com.gd.grdrpc.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @Author by guanda
 * @Date 2018/7/30 16:18
 */
@Configuration
@PropertySource("classpath:grdRpcClient.properties")
public class GrdRpcClientProperties {
}
