package com.leyou.auth.task;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.mapper.ApplicationInfoMapper;
import com.leyou.common.auth.entity.AppInfo;
import com.leyou.common.auth.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 虎哥
 */
@Slf4j
@Component
public class PrivilegeTokenHolder {

    private String token;

    /**
     * token刷新间隔
     */
    private static final long TOKEN_REFRESH_INTERVAL = 86400000L;

    /**
     * token获取失败后重试的间隔
     */
    private static final long TOKEN_RETRY_INTERVAL = 10000L;

    @Autowired
    private JwtProperties prop;

    @Autowired
    private ApplicationInfoMapper infoMapper;

    /**
     * 定时任务，每隔指定时间，项目启动会先执行一次，执行一次，
     */
    @Scheduled(fixedDelay = TOKEN_REFRESH_INTERVAL)
    public void getTokenTask() throws InterruptedException {
        while (true) {
            try {
                // 查询权限服务列表
                List<Long> targetIdList = infoMapper.queryTargetIdList(prop.getApp().getId());
                // 封装载荷
                AppInfo appInfo = new AppInfo();
                appInfo.setId(prop.getApp().getId());
                appInfo.setServiceName(prop.getApp().getServiceName());
                appInfo.setTargetList(targetIdList);
                // 生成token
                this.token = JwtUtils.generateTokenExpireInMinutes(appInfo, prop.getPrivateKey(), prop.getApp().getExpire());
                // 成功，结束循环
                log.info("授权中心生成token成功，token:[{}]", token);
                break;
            } catch (Exception e) {
                log.info("授权中心生成token失败！原因:{}", e.getMessage(), e);
                // 休眠一定时间后，再次重试获取token
                Thread.sleep(TOKEN_RETRY_INTERVAL);
            }
        }


    }

    public String getToken() {
        return token;
    }
}
