package com.sky.config.security;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "sky.security")
public class SecurityProperties {
    private List<String> permitUrls;

    public List<String> getPermitUrls() {
        return permitUrls;
    }

    public void setPermitUrls(List<String> permitUrls) {
        this.permitUrls = permitUrls;
    }


}
