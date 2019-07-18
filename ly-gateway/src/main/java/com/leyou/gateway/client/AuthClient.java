package com.leyou.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author 虎哥
 */
@FeignClient("auth-service")
public interface AuthClient {
    /**
     * 微服务授权接口
     *
     * @param id
     * @param secret
     * @return
     */
    @GetMapping("authorization")
    String authorize(@RequestParam("id") Long id, @RequestParam("secret") String secret);
}
