package com.share.order.api;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.constant.ServiceNameConstants;
import com.share.common.core.domain.R;
import com.share.order.domain.OrderInfo;
import com.share.order.domain.OrderSqlVo;
import com.share.order.factory.RemoteOrderInfoFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务
 *
 */
@FeignClient(contextId = "remoteOrderInfoService", value = ServiceNameConstants.ORDER_SERVICE, fallbackFactory = RemoteOrderInfoFallbackFactory.class)

    // 远程订单服务 Feign 接口：获取未完成订单、根据订单号查询、订单统计
public interface RemoteOrderInfoService {


    // 查询用户未完成订单（充电中 status=0 或待支付 status=1）
    @GetMapping("/orderInfo/getNoFinishOrder/{userId}")
    public R<OrderInfo> getNoFinishOrder(@PathVariable("userId") Long userId);


    // 根据订单号精确查询订单
    @GetMapping("/orderInfo/getByOrderNo/{orderNo}")
    public R<OrderInfo> getByOrderNo(@PathVariable("orderNo") String orderNo, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);


    // 执行 AI 生成的 SQL 查询订单报表数据，动态识别列名适配前端图表
    @PostMapping(value = "/orderInfo/getOrderCount")
    public R getOrderCount(@RequestBody OrderSqlVo orderSqlVo);
}
