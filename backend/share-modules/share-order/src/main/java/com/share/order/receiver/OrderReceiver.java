package com.share.order.receiver;

import com.alibaba.fastjson2.JSONObject;
import com.rabbitmq.client.Channel;
import com.share.common.rabbit.constant.MqConst;
import com.share.order.domain.EndOrderVo;
import com.share.order.domain.SubmitOrderVo;
import com.share.order.service.IOrderInfoService;
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

    // 订单消息监听器：监听 RabbitMQ 租借/归还/支付消息，防重处理 + 手动应答
public class OrderReceiver {

    @Autowired
    private IOrderInfoService orderInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_SUBMIT_ORDER, durable = "true"),
            key = MqConst.ROUTING_SUBMIT_ORDER
    ))
    public void submitOrder(String content, Message message, Channel channel) {
        log.info("[订单服务]租借充电宝消息：{}", content);
        SubmitOrderVo orderForm = JSONObject.parseObject(content, SubmitOrderVo.class);
        String messageNo = orderForm.getMessageNo();
        //防止重复请求
        String key = "order:submit:" + messageNo;
        boolean isExist = redisTemplate.opsForValue().setIfAbsent(key, messageNo, 1, TimeUnit.HOURS);
        if (!isExist) {
            log.info("重复请求: {}", content);
            return;
        }

        try {

    // 创建新订单：随机订单号 → 绑定充电宝 → 记录借用站点 → 设置费用规则 → 订单状态设为充电中
            orderInfoService.saveOrder(orderForm);

            //手动应答
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("订单服务：订单归还失败，订单编号：{}", messageNo, e);
            redisTemplate.delete(key);
            // 消费异常，重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }


    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_END_ORDER, durable = "true"),
            key = MqConst.ROUTING_END_ORDER
    ))

    // 结束订单（归还充电宝）：计算充电时长 → Drools 规则引擎计算费用 → 设置订单金额 → 插入账单明细 → 更新订单状态
    public void endOrder(String content, Message message, Channel channel) {
        log.info("[订单服务]归还充电宝消息：{}", content);
        EndOrderVo endOrderVo = JSONObject.parseObject(content, EndOrderVo.class);
        String messageNo = endOrderVo.getMessageNo();
        //防止重复请求
        String key = "order:endOrder:" + messageNo;
        boolean isExist = redisTemplate.opsForValue().setIfAbsent(key, messageNo, 1, TimeUnit.HOURS);
        if (!isExist) {
            log.info("重复请求: {}", content);
            return;
        }

        try {
            orderInfoService.endOrder(endOrderVo);

            //手动应答
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("订单服务：订单归还失败，订单编号：{}", messageNo, e);
            redisTemplate.delete(key);
            // 消费异常，重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}