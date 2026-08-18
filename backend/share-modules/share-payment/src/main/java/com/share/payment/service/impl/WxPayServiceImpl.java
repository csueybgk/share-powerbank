package com.share.payment.service.impl;

import com.share.payment.domain.CreateWxPaymentForm;
import com.share.payment.domain.WxPrepayVo;
import com.share.payment.service.IWxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WxPayServiceImpl implements IWxPayService {


    @Override

    // 微信 JSAPI 下单：保存支付记录 → 获取用户 openid → 调用微信 v3 API → 返回预支付参数给小程序
    public WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm) {
        return null;

    }

}