package com.share.rules.factory;

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
public class RemoteFeeRuleFallbackFactory implements FallbackFactory<RemoteFeeRuleFallbackFactory>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteFeeRuleFallbackFactory.class);

    @Override
    public RemoteFeeRuleFallbackFactory create(Throwable throwable)
    {
        log.error("用户服务调用失败:{}", throwable.getMessage());
        throw new ServiceException("获取用户信息失败");


    }
}
