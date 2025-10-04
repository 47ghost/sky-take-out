package com.sky.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;


//创建 WebSocket 端点的过程是由一个名为 ServerEndpointExporter 的 Bean 来处理的。
// 这个过程独立于 Spring MVC 的 DispatcherServlet
@Configuration
public class WebSocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

}
