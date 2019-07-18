package com.leyou.order.utils;

import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConfigImpl;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 虎哥
 */
@Slf4j
@Component
public class WxPayHelper {

    @Autowired
    private WXPay wxPay;

    @Autowired
    private WXPayConfigImpl payConfig;

    /**
     * 统一下单，获取支付链接
     *
     * @return
     */
    public String getPayUrl(Long orderId, String desc, Long totalFee) {
        // 6个请求参数：
        Map<String, String> data = new HashMap<String, String>();
        data.put("body", desc);
        data.put("out_trade_no", orderId.toString());
        data.put("total_fee", totalFee.toString());
        data.put("spbill_create_ip", payConfig.getSpbillCreateIp());
        data.put("trade_type", payConfig.getTradeType());  // 此处指定为扫码支付

        try {
            // 发起请求，得到响应
            Map<String, String> resp = wxPay.unifiedOrder(data);
            // 校验通信标示
            if ("FAIL".equals(resp.get("return_code"))) {
                // 通信失败！
                throw new RuntimeException("微信系统通信失败！");
            }
            // 校验业务标示
            checkResultCode(resp);
            // 校验签名
            checkSignature(resp);

            // 获取url地址
            String codeUrl = resp.get("code_url");
            if (StringUtils.isBlank(codeUrl)) {
                throw new RuntimeException("支付地址为空！");
            }
            return codeUrl;
        } catch (Exception e) {
            // 下单失败
            log.error("【微信支付】统一下单失败！原因：{}", e.getMessage());
            throw new LyException(ExceptionEnum.WXPAY_UNIFIED_ORDER_ERROR);
        }
    }

    public void checkSignature(Map<String, String> resp) {
        try {
            boolean boo1 = WXPayUtil.isSignatureValid(resp, payConfig.getKey());
            boolean boo2 = WXPayUtil.isSignatureValid(resp, payConfig.getKey(), WXPayConstants.SignType.HMACSHA256);
            if (!boo1 && !boo2) {
                throw new RuntimeException();
            }
        } catch (Exception e) {
            throw new RuntimeException("非法签名！我要报警了！");
        }
    }

    public void checkResultCode(Map<String, String> resp) {
        // 校验业务标示
        if ("FAIL".equals(resp.get("result_code"))) {
            // 业务失败
            throw new RuntimeException(resp.get("err_code_des"));
        }
    }
}
