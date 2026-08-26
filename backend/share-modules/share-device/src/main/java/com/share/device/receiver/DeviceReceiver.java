package com.share.device.receiver;

import com.alibaba.fastjson2.JSONObject;
import com.rabbitmq.client.Channel;
import com.share.common.rabbit.constant.MqConst;
import com.share.device.domain.CabinetSlot;
import com.share.device.service.IDeviceService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component

    // 设备消息监听器：监听 RabbitMQ 设备相关消息
public class DeviceReceiver {

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private RedisTemplate redisTemplate;

    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_UNLOCK_SLOT)

    // 解锁卡槽：延迟消息回调，若卡槽仍为锁定状态则恢复为空闲
    public void unlockSlot(String content, Message message, Channel channel) {
        log.info("[设备服务]解锁充电宝卡槽消息：{}", content);
        CabinetSlot cabinetSlot = JSONObject.parseObject(content, CabinetSlot.class);
        //防止重复请求
        String key = "unlock:slot:" + cabinetSlot.getCabinetId() + ":" + cabinetSlot.getSlotNo();
        boolean isExist = redisTemplate.opsForValue().setIfAbsent(key, cabinetSlot.getSlotNo(), 1, TimeUnit.HOURS);
        if (!isExist) {
            log.info("重复请求: {}", content);
            // 已处理过的重复消息直接确认，避免消息一直不被 ack 悬挂在消费者上
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        try {
            deviceService.unlockSlot(cabinetSlot);

            //手动应答
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("设备服务：解锁充电宝卡槽失败：{}", content, e);
            // 删除幂等标记，允许容器重试时重新执行
            redisTemplate.delete(key);
            // 抛出异常交给容器 RetryTemplate 重试（最多 3 次），耗尽后 requeue=false 进死信队列，不再无限重投
            throw e;
        }
    }

    /**
     * 死信队列：重试耗尽仍失败的卡槽解锁消息，人工排查
     */
    @SneakyThrows
    @RabbitListener(queues = "share.unlock.slot.dead")
    public void deadLetterSlot(String content, Message message, Channel channel) {
        log.error("[设备服务]卡槽解锁死信消息（重试耗尽）：{}", content);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }


}
