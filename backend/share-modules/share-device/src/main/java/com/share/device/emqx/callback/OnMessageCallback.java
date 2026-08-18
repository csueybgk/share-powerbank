package com.share.device.emqx.callback;

import com.alibaba.fastjson2.JSONObject;
import com.share.device.emqx.factory.MessageHandlerFactory;
import com.share.device.emqx.handler.MassageHandler;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component

    // MQTT 消息回调接口，收到消息后通过 MessageHandlerFactory 分发到对应 Handler
public class OnMessageCallback implements MqttCallback {

    @Autowired
    private MessageHandlerFactory messageHandlerFactory;

    @Override
    public void connectionLost(Throwable cause) {
        // 连接丢失后，一般在这里面进行重连
        System.out.println("连接断开，可以做重连");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // subscribe后得到的消息会执行到这里面
        System.out.println("接收消息主题:" + topic);
        System.out.println("接收消息Qos:" + message.getQos());
        System.out.println("接收消息内容:" + new String(message.getPayload()));
        try {
            // 根据主题选择不同的处理逻辑
            MassageHandler massageHandler = messageHandlerFactory.getMassageHandler(topic);
            if(null != massageHandler) {
                String content = new String(message.getPayload());

    // MQTT 消息统一处理方法
                massageHandler.handleMessage(JSONObject.parseObject(content));
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("mqtt消息异常：{}", new String(message.getPayload()));
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("deliveryComplete---------" + token.isComplete());
    }
}
