package com.leyou.order.web;

import com.leyou.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 虎哥
 */
@RestController
@RequestMapping("pay")
public class PayController {

    @Autowired
    private OrderService orderService;

    /**
     * 微信支付的回调
     *
     * @param data
     * @return
     */
    @PostMapping(value = "/wxpay/notify", produces = "application/xml")
    public ResponseEntity<Map<String, String>> handleWXNotify(@RequestBody Map<String, String> data) {
        orderService.handleNotify(data);
        // 定义结果
        Map<String, String> result = new HashMap<>();
        result.put("return_code", "SUCCESS");
        result.put("return_msg", "OK");
        return ResponseEntity.ok(result);
    }
}
