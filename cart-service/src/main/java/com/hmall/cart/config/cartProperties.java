package com.hmall.cart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * <p>Project: hmall</p>
 * <p>Date: 2025/8/25 17:13</p>
 * Description:
 */
@Data
@Component
@ConfigurationProperties(prefix = "hm.cart") // 匹配 hm.cart.maxItems
public class cartProperties {
    private Integer maxItems; // 定义配置文件中的属性 hm.cart.maxItems
}
