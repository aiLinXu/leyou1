package com.leyou.auth.config;

import com.leyou.common.auth.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author 虎哥
 */
@Slf4j
@Data
@ConfigurationProperties("ly.jwt")
public class JwtProperties implements InitializingBean {
    /**
     * 公钥地址
     */
    private String pubKeyPath;
    /**
     * 私钥地址
     */
    private String priKeyPath;
    /**
     * 公钥对象
     */
    private PublicKey publicKey;
    /**
     * 公钥对象
     */
    private PrivateKey privateKey;

    private UserProperties user = new UserProperties();

    @Data
    public class UserProperties {
        private int expire;
        private String cookieName;
        private String domain;
        private long maxRemainTime;
    }

    private AppProperties app = new AppProperties();

    @Data
    public class AppProperties {
        private int expire;
        private long id;
        private String serviceName;
        private String headerName;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            // 加载公钥
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            // 加载私钥
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            log.error("【授权中心】加载公钥和私钥失败！原因：{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
