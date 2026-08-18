package com.share.device.emqx.handler;

import com.alibaba.fastjson2.JSONObject;

public interface MassageHandler {

    // MQTT 消息统一处理方法
    void handleMessage(JSONObject jsonObject);
}
