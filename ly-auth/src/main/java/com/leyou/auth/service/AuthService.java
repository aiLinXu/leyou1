package com.leyou.auth.service;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.entity.ApplicationInfo;
import com.leyou.auth.mapper.ApplicationInfoMapper;
import com.leyou.common.auth.entity.AppInfo;
import com.leyou.common.auth.entity.Payload;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.CookieUtils;
import com.leyou.user.client.UserClient;
import com.leyou.user.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 虎哥
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties prop;

    public void login(String username, String password, HttpServletResponse response) {
        try {
            // 1、拿用户名和密码去用户中心查询
            UserDTO user = userClient.queryUserByUsernameAndPassword(username, password);
            // 2、判断是否查到
            if (user == null) {
                throw new RuntimeException("用户名或密码错误");
            }
            // 3、如果有，利用私钥，生成TOKEN
            // 3.1.用户数据
            UserInfo userInfo = new UserInfo(user.getId(), user.getUsername(), "GUEST");
            // 3.2.生成token
            String token = JwtUtils.generateTokenExpireInMinutes(
                    userInfo, prop.getPrivateKey(), prop.getUser().getExpire());
            // 4、把token返回给用户, 写入cookie
            writeCookie(response, token);
        } catch (Exception e) {
            log.error("用户登录失败，原因：{}", e.getMessage());
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
    }

    private void writeCookie(HttpServletResponse response, String token) {
        CookieUtils.newCookieBuilder()
                .name(prop.getUser().getCookieName())
                .value(token)
                .domain(prop.getUser().getDomain())
                .httpOnly(true)
                .response(response)
                .build();
    }

    public UserInfo verify(HttpServletRequest request, HttpServletResponse response) {
        // 1.获取cookie中的token
        String token = CookieUtils.getCookieValue(request, prop.getUser().getCookieName());
        try {
            // 2. 解析token
            Payload<UserInfo> payload = JwtUtils.getInfoFromToken(token, prop.getPublicKey(), UserInfo.class);
            // 3.校验黑名单
            Boolean boo = redisTemplate.hasKey(payload.getId());
            if (boo != null && boo) {
                // 证明存在与黑名单，说明token已经登出过，无效
                throw new RuntimeException("token已经登出，是无效token");
            }
            // 4. token续期
            // 4.1.取出过期时间
            Date expiration = payload.getExpiration();
            // 4.2.获取剩余有效期 到期时间 - 现在时间 < 10
            long remainTime = expiration.getTime() - System.currentTimeMillis();
            // 4.3.判断token过期时间距离现在是否小于10分钟
            if (remainTime < prop.getUser().getMaxRemainTime()) {
                // 4.4.如果小于，则重新生成token
                String newToken = JwtUtils.generateTokenExpireInMinutes(
                        payload.getUserInfo(), prop.getPrivateKey(), prop.getUser().getExpire());
                // 4.5.把token写到cookie
                writeCookie(response, token);
            }
            // 5.返回用户信息
            return payload.getUserInfo();
        } catch (Exception e) {
            // token无效或者已过期
            log.error("校验登录状态失败，原因：{}", e.getMessage(), e);
            throw new LyException(ExceptionEnum.UNAUTHORIZED, e);
        }
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            //	1.获取token
            String token = CookieUtils.getCookieValue(request, prop.getUser().getCookieName());
            if (token == null) {
                // 用户未登录，无需后续操作
                throw new RuntimeException("用户尚未登录！");
            }
            Payload<Object> payload;
            try {
                //	2.解析token，获取过期时间，获取id
                payload = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            } catch (Exception e) {
                throw new RuntimeException("登录已经失效！");
            }
            // 2.1.获取过期时间
            Date expiration = payload.getExpiration();
            // 2.2.获取id
            String id = payload.getId();

            //	3.记录id到redis，设置有效期为token剩余有效期
            // 3.1.获取剩余有效期
            long remainTime = expiration.getTime() - System.currentTimeMillis();
            // 3.2.写到redis
            redisTemplate.opsForValue().set(id, "1", remainTime, TimeUnit.MILLISECONDS);

            //	4.删除cookie
            CookieUtils.deleteCookie(prop.getUser().getCookieName(), prop.getUser().getDomain(), response);
        } catch (Exception e) {
            log.error("用户登出失败，原因：{}", e.getMessage());
            throw new LyException(ExceptionEnum.UNAUTHORIZED, e);
        }
    }

    @Autowired
    private ApplicationInfoMapper infoMapper;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public String authorize(Long id, String secret) {
        try {
            // 1.校验服务身份
            // 1.1.根据id查询applicationInfo
            ApplicationInfo app = infoMapper.selectByPrimaryKey(id);
            // 1.2.判断是否查询到
            if (app == null) {
                // id或secret错误
                throw new RuntimeException("服务id或密码错误！");
            }
            // 1.3.校验secret（PasswordEncoder）
            if (!passwordEncoder.matches(secret, app.getSecret())) {
                throw new RuntimeException("服务id或密码错误！");
            }
            //	2.生成JWT
            // 2.1.查询服务的权限
            List<Long> targetIdList = infoMapper.queryTargetIdList(app.getId());
            // 2.2.生成载荷
            AppInfo appInfo = new AppInfo();
            appInfo.setId(app.getId());
            appInfo.setServiceName(app.getServiceName());
            appInfo.setTargetList(targetIdList);
            // 2.3.生成token
            return JwtUtils.generateTokenExpireInMinutes(appInfo, prop.getPrivateKey(), prop.getApp().getExpire());
        } catch (Exception e) {
            log.error("服务授权认证失败，原因：{}", e.getMessage(), e);
            throw new LyException(ExceptionEnum.UNAUTHORIZED, e);
        }
    }
}
