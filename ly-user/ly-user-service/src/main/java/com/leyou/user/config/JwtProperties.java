package com.leyou.user.config;

import com.leyou.common.auth.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
     * 公钥对象
     */
    private PublicKey publicKey;

    private AppProperties app = new AppProperties();

    @Data
    public class AppProperties {
        private long id;
        private String secret;
        private String headerName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            // 加载公钥
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            log.error("【用户服务】加载公钥失败！原因：{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
