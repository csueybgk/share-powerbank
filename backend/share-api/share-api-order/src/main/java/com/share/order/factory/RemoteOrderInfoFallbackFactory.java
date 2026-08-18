package com.share.order.factory;

import com.share.common.core.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务降级处理
 *
 */
@Component
public class RemoteOrderInfoFallbackFactory implements FallbackFactory<RemoteOrderInfoFallbackFactory>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteOrderInfoFallbackFactory.class);

    @Override
    public RemoteOrderInfoFallbackFactory create(Throwable throwable)
    {
        log.error("用户服务调用失败:{}", throwable.getMessage());
        throw new ServiceException("获取用户信息失败");


    }
}
