package com.sky.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.json.JacksonObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/*继承 WebMvcConfigurationSupport 会禁用大量 Spring Boot 默认自动配置（包括 Jackson 默认配置、内容协商、OpenAPI 相关配置等），这是最常见踩坑点
方案 A（最简，保持 Boot 自动配置）: 删除整个 WebMvcConfigurationSupport 类，改为提供一个 ObjectMapper Bean。Spring Boot 会自动使用它*/
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        // 自定义格式已在 JacksonObjectMapper 中完成
        return new JacksonObjectMapper();
    }

}
