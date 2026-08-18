package com.share.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.order.domain.EndOrderVo;
import com.share.order.domain.OrderInfo;
import com.share.order.domain.SubmitOrderVo;

import java.util.List;
import java.util.Map;

public interface IOrderInfoService extends IService<OrderInfo> {

    // 查询用户未完成订单（充电中 status=0 或待支付 status=1）
    OrderInfo getNoFinishOrder(Long userId);



    // 创建新订单：随机订单号 → 绑定充电宝 → 记录借用站点 → 设置费用规则 → 订单状态设为充电中
    Long saveOrder(SubmitOrderVo orderForm);


    // 结束订单（归还充电宝）：计算充电时长 → Drools 规则引擎计算费用 → 设置订单金额 → 插入账单明细 → 更新订单状态
    void endOrder(EndOrderVo endOrderVo);


    // 查询用户订单列表，充电中的订单实时计算使用时长和费用
    List<OrderInfo> selectUserOrderInfoList(Long userId);


    // 查询订单详情，包含账单列表和用户信息
    OrderInfo selectOrderInfoById(Long id);


    // 根据订单号精确查询订单
    OrderInfo getByOrderNo(String orderNo);


    // 本地开发模拟支付成功：跳过微信支付流程，直接标记订单为已支付
    void simulatePaySuccess(String orderNo);


    // 执行 AI 生成的 SQL 查询订单报表数据，动态识别列名适配前端图表
    Map<String, Object> getOrderCount(String sql);


    // 后台管理端查询订单列表，支持按订单号、充电宝编号、状态筛选
    List<OrderInfo> selectOrderInfoList(OrderInfo orderInfo);


}
