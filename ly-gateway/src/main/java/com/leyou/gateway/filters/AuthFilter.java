package com.leyou.gateway.filters;

import com.leyou.common.auth.entity.Payload;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.gateway.config.FilterProperties;
import com.leyou.gateway.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 虎哥
 */
@Slf4j
@Component
public class AuthFilter extends ZuulFilter {

    @Autowired
    private JwtProperties prop;

    @Autowired
    private FilterProperties filterProp;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 过滤器类型，前置
     *
     * @return
     */
    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.FORM_BODY_WRAPPER_FILTER_ORDER + 1;
    }

    /**
     * @return true：run方法会被执行。false：不执行run方法
     */
    @Override
    public boolean shouldFilter() {
        // 1.获取上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        // 2.获取request
        HttpServletRequest request = ctx.getRequest();
        // 3.获取请求路径
        String path = request.getRequestURI();
        // 4.判断path是否在白名单中
        boolean isAllow = isAllowPath(path);
        // 需要放行的路径，这里返回false，需要拦截的路径，这里返回true
        return !isAllow;
    }

    private boolean isAllowPath(String path) {
        // 获取白名单中的路径
        List<String> allowPaths = filterProp.getAllowPaths();
        // 遍历白名单
        for (String allowPath : allowPaths) {
            // 判断前缀是否匹配
            if (path.startsWith(allowPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object run() throws ZuulException {
        // 1.获取上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        // 2.获取request
        HttpServletRequest request = ctx.getRequest();
        // 3.获取cookie中的token
        String token = CookieUtils.getCookieValue(request, prop.getUser().getCookieName());
        try {
            // 4. 解析token
            Payload<UserInfo> payload = JwtUtils.getInfoFromToken(token, prop.getPublicKey(), UserInfo.class);
            // 5.校验黑名单
            Boolean boo = redisTemplate.hasKey(payload.getId());
            if (boo != null && boo) {
                // 证明存在与黑名单，说明token已经登出过，无效
                throw new RuntimeException("token已经登出，是无效token");
            }
            // 6.权限控制
            UserInfo user = payload.getUserInfo();
            // 获取用户角色，查询权限
            String role = user.getRole();
            // 获取当前资源路径
            String path = request.getRequestURI();
            String method = request.getMethod();
            // TODO 判断权限，此处暂时空置，等待权限服务完成后补充
            log.info("【网关】用户{},角色{}。访问服务{} : {}，", user.getUsername(), role, method, path);
        } catch (Exception e) {
            // 拦截请求
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(403);// Forbidden
            log.error("非法访问，未登录，地址：{}", request.getRemoteHost(), e);
        }
        return null;
    }
}
