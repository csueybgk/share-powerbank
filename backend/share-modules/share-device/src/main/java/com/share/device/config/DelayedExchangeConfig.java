package com.share.device.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
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

    /** 死信交换机：消费重试耗尽的消息进入死信队列，避免无限重试 */
    public static final String EXCHANGE_DEVICE_DLX = "share.device.dlx";
    public static final String QUEUE_UNLOCK_SLOT_DEAD = "share.unlock.slot.dead";
    public static final String ROUTING_UNLOCK_SLOT_DEAD = "share.unlock.slot.dead";

    @Bean
    public CustomExchange deviceDelayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(EXCHANGE_DEVICE_DELAYED, DELAYED_EXCHANGE_TYPE, true, false, args);
    }

    @Bean
    public Queue deviceUnlockSlotQueue() {
        Map<String, Object> args = new HashMap<>();
        // 重试耗尽（容器 RetryTemplate 3 次）仍失败的消息，不再无限重投，进死信队列人工排查
        args.put("x-dead-letter-exchange", EXCHANGE_DEVICE_DLX);
        args.put("x-dead-letter-routing-key", ROUTING_UNLOCK_SLOT_DEAD);
        return new Queue("share.unlock.slot", true, false, false, args);
    }

    @Bean
    public Binding deviceUnlockSlotBinding() {
        return BindingBuilder
                .bind(deviceUnlockSlotQueue())
                .to(deviceDelayedExchange())
                .with("share.unlock.slot")
                .noargs();
    }

    // ========== 死信队列（DLQ）==========
    @Bean
    public DirectExchange deviceDeadLetterExchange() {
        return new DirectExchange(EXCHANGE_DEVICE_DLX);
    }

    @Bean
    public Queue deviceUnlockSlotDeadQueue() {
        return new Queue(QUEUE_UNLOCK_SLOT_DEAD, true);
    }

    @Bean
    public Binding deviceUnlockSlotDeadBinding() {
        return BindingBuilder
                .bind(deviceUnlockSlotDeadQueue())
                .to(deviceDeadLetterExchange())
                .with(ROUTING_UNLOCK_SLOT_DEAD);
    }
}
