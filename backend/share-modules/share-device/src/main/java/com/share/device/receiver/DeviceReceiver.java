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
            return;
        }

        try {
            deviceService.unlockSlot(cabinetSlot);

            //手动应答
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("设备服务：解锁充电宝卡槽失败：{}", content, e);
            redisTemplate.delete(key);
            // 消费异常，重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }


}
