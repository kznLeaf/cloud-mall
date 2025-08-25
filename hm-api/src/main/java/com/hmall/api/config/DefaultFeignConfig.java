package com.hmall.api.config;

import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

/**
 * <p>Project: hmall</p>
 * <p>Date: 2025/8/24 14:29</p>
 * Description:
 */
public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 给发出的请求加上用户id。匿名创建一个新的 RequestInterceptor ，重写 apply 方法，在方法里面把请求头加上去
     *
     * @return 一个 openFeign 的请求拦截器
     */
    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                Long id = UserContext.getUser();
                if (id != null)
                    requestTemplate.header("user-info", id.toString());
            }
        };
    }
}
