package com.hmall.common.config;

import com.hmall.common.interceptors.UserInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.Serializable;

/**
 * <p>Project: hmall</p>
 * <p>Date: 2025/8/25 14:12</p>
 * Description:
 */
@Configuration
@ConditionalOnClass(DispatcherServlet.class)
public class MVCconfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserInfo()); // 默认拦截所有的路径
    }
}
