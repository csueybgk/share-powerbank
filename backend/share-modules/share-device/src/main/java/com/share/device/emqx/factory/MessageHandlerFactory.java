package com.share.device.emqx.factory;

import com.share.device.emqx.handler.MassageHandler;

public interface MessageHandlerFactory {
    MassageHandler getMassageHandler(String topic);
}
