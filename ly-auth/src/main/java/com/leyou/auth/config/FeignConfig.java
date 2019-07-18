package com.leyou.auth.config;

import com.leyou.auth.task.PrivilegeTokenHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 虎哥
 */
@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor requestInterceptor(JwtProperties prop, PrivilegeTokenHolder tokenHolder) {
        return new PrivilegeRequestInterceptor(prop, tokenHolder);
    }

    class PrivilegeRequestInterceptor implements RequestInterceptor {

        private JwtProperties prop;

        private PrivilegeTokenHolder tokenHolder;

        public PrivilegeRequestInterceptor(JwtProperties prop, PrivilegeTokenHolder tokenHolder) {
            this.prop = prop;
            this.tokenHolder = tokenHolder;
        }

        @Override
        public void apply(RequestTemplate template) {
            template.header(prop.getApp().getHeaderName(), tokenHolder.getToken());
        }
    }
}
