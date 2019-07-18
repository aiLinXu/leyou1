package com.leyou.cart.interceptors;

import com.leyou.common.utils.UserHolder;
import com.leyou.common.auth.entity.Payload;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.CookieUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;

/**
 * @author 虎哥
 */
@Slf4j
public class UserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取token
        String token = CookieUtils.getCookieValue(request, "LY_TOKEN");
        // 解析用户信息
        try {
            Payload<UserInfo> payload = JwtUtils.getInfoFromToken(token, UserInfo.class);
            // 获取其中的用户
            UserInfo user = payload.getUserInfo();
            // 存储用户
            UserHolder.setUser(user);
            // 放行
            return true;
        } catch (UnsupportedEncodingException e) {
            // 解析用户信息失败
            log.error("解析用户信息失败，原因：{}", e.getMessage());
            throw new LyException(ExceptionEnum.UNAUTHORIZED, e);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 移除用户
        UserHolder.remove();
    }
}
