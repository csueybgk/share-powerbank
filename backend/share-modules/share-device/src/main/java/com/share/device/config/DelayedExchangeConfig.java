package com.share.device.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 设备模块延迟交换机配置
 * 类型为 x-delayed-message，支持消息延迟投递
 * 用于扫码后未弹出充电宝的延迟解锁卡槽场景
 */
@Configuration
public class DelayedExchangeConfig {

    public static final String EXCHANGE_DEVICE_DELAYED = "share.device";
    public static final String DELAYED_EXCHANGE_TYPE = "x-delayed-message";

    @Bean
    public CustomExchange deviceDelayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(EXCHANGE_DEVICE_DELAYED, DELAYED_EXCHANGE_TYPE, true, false, args);
    }

    @Bean
    public Queue deviceUnlockSlotQueue() {
        return new Queue("share.unlock.slot", true);
    }

    @Bean
    public Binding deviceUnlockSlotBinding() {
        return BindingBuilder
                .bind(deviceUnlockSlotQueue())
                .to(deviceDelayedExchange())
                .with("share.unlock.slot")
                .noargs();
    }
}
