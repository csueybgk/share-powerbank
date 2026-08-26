package com.share.common.rabbit.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * RabbitMQ 消费端重试 + 死信兜底配置
 *
 * <p>背景：默认情况下消费者失败时 {@code basicNack(..., requeue=true)} 会把消息放回队头立即重投，
 * 一条一直失败的消息会无限重试并占住队头（毒消息问题），后续正常消息全部排队等待。</p>
 *
 * <p>本配置给所有 {@code @RabbitListener} 统一加上：</p>
 * <ol>
 *     <li>容器内重试：同一消息最多消费 3 次（1s 起、2 倍指数退避，最长 10s），短暂故障自动恢复；</li>
 *     <li>重试耗尽后 {@code requeue=false}：消息不再重回队列，配合业务队列上声明的
 *     {@code x-dead-letter-exchange} 进入死信队列，由人工排查，不拖垮整条队列。</li>
 * </ol>
 *
 * <p>覆盖 Spring Boot 自动装配的 {@code rabbitListenerContainerFactory}（同名 Bean 抢占生效），
 * 通过 {@link SimpleRabbitListenerContainerFactoryConfigurer} 沿用 Boot 的连接/并发/预取等默认配置。</p>
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(RabbitAutoConfiguration.class)
public class RabbitListenerRetryConfig {

    /**
     * 统一的监听器容器工厂：手动 ack + 消费重试（3 次）+ 重试耗尽不重回队列
     */
    @Bean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        // 沿用 Spring Boot 默认配置（连接工厂、并发、Prefetch 等）
        configurer.configure(factory, connectionFactory);
        // 手动 ack：与现有消费者代码（channel.basicAck / basicNack）保持一致
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // 消费重试：最多 3 次，1s 起 + 2 倍指数退避
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);
        retryTemplate.setBackOffPolicy(backOff);
        factory.setRetryTemplate(retryTemplate);

        // 重试耗尽后消息不重回队列（requeue=false），由死信交换机接管
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}
