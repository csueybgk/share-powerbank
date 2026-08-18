package com.share.payment.service;

import com.share.payment.domain.CreateWxPaymentForm;
import com.share.payment.domain.WxPrepayVo;

public interface IWxPayService {


    // 微信 JSAPI 下单：保存支付记录 → 获取用户 openid → 调用微信 v3 API → 返回预支付参数给小程序
    WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm);

}