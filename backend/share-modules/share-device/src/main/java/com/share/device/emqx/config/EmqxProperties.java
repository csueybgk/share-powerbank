package com.share.device.emqx.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "emqx.client")

    // EMQX 连接配置属性：host、port、username、password、clientId
public class EmqxProperties {

    private String clientId;
    private String username;
    private String password;
    private String serverURI;
    private int keepAliveInterval;
    private int connectionTimeout;
}
