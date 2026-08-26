package com.share.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单模块死信交换机 / 死信队列配置
 *
 * <p>订单业务队列（share.submit.order / share.end.order）通过队列参数
 * {@code x-dead-letter-exchange} 指向本死信交换机：消费端重试（最多 3 次）耗尽仍失败的消息，
 * 以 requeue=false 拒绝后进入对应死信队列，由人工排查，不再无限重投占住队头。</p>
 */
@Configuration
public class DeadLetterOrderConfig {

    public static final String EXCHANGE_ORDER_DLX = "share.order.dlx";
    public static final String QUEUE_SUBMIT_ORDER_DEAD = "share.submit.order.dead";
    public static final String QUEUE_END_ORDER_DEAD = "share.end.order.dead";

    @Bean
    public DirectExchange orderDeadLetterExchange() {
        return new DirectExchange(EXCHANGE_ORDER_DLX);
    }

    @Bean
    public Queue submitOrderDeadQueue() {
        return new Queue(QUEUE_SUBMIT_ORDER_DEAD, true);
    }

    @Bean
    public Queue endOrderDeadQueue() {
        return new Queue(QUEUE_END_ORDER_DEAD, true);
    }

    @Bean
    public Binding submitOrderDeadBinding() {
        return BindingBuilder.bind(submitOrderDeadQueue()).to(orderDeadLetterExchange()).with(QUEUE_SUBMIT_ORDER_DEAD);
    }

    @Bean
    public Binding endOrderDeadBinding() {
        return BindingBuilder.bind(endOrderDeadQueue()).to(orderDeadLetterExchange()).with(QUEUE_END_ORDER_DEAD);
    }
}
