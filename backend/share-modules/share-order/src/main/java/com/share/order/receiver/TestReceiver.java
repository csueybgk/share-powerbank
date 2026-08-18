package com.share.order.receiver;



import com.rabbitmq.client.Channel;
import com.share.common.rabbit.constant.MqConst;
import com.share.order.config.DeadLetterMqConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TestReceiver {
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 监听消息
     * @param content
     * @param message
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_TEST, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_TEST, durable = "true"),
            key = MqConst.ROUTING_TEST
    ))
    public void test(String content, Message message) {
        log.info("接收消息：{}", content);
        log.info("接收消息：{}", new String(message.getBody()));
    }

    /**
     * 监听确认消息
     * @param message
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_TEST, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_CONFIRM, durable = "true"),
            key = MqConst.ROUTING_CONFIRM
    ))
    public void confirm(String content, Message message, Channel channel) {
        log.info("接收确认消息：{}", content);

        // false 确认一个消息，true 批量确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    /**
     * 监听延迟消息
     * @param msg
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(queues = {DeadLetterMqConfig.queue_dead_2})
    public void getDeadLetterMsg(String msg, Message message, Channel channel) {
        //接收消息，消费者端判断是否需要做幂等性处理
        //如果业务保证幂等性，基于redis setnx保证
        String key = "mq:" + msg;
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, "", 200, TimeUnit.SECONDS);
        if (!flag) {
            //说明该业务数据以及被执行
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        // 执行业务
        //  TODO

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}