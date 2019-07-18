package com.leyou.sms.mq;

import com.leyou.common.constants.MQConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SmsListenerTest {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Test
    public void testSend(){
        String code = RandomStringUtils.randomNumeric(6);
        System.out.println("code = " + code);
        Map<String, String> msg = new HashMap<>();
        msg.put("phone", "14752169639");
        msg.put("code", code);
        amqpTemplate.convertAndSend(
                MQConstants.Exchange.SMS_EXCHANGE_NAME, MQConstants.RoutingKey.VERIFY_CODE_KEY, msg);
    }

}