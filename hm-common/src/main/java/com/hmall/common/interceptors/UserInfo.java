package com.hmall.common.interceptors;

import com.hmall.common.utils.UserContext;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Project: hmall</p>
 * <p>Date: 2025/8/25 14:00</p>
 * Description:
 *
 * 一个拦截器，把用户信息存入各自的 ThreadLocal
 *
 * 因为不同的服务都引用了这段代码，然后每个服务内都创建了一个thread local实例
 */
public class UserInfo implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userinfo = request.getHeader("user-info"); // 实际上是一串数字
        if(StringUtils.hasLength(userinfo)) {
            UserContext.setUser(Long.valueOf(userinfo));
        }

        return true; // 没有通过验证就不放行 TODO
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.removeUser();
    }
}
