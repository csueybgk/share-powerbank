package com.share.order.service.impl;


import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.common.core.utils.bean.BeanUtils;
import com.share.common.security.utils.SecurityUtils;
import com.share.order.domain.*;
import com.share.order.mapper.OrderBillMapper;
import com.share.order.mapper.OrderInfoMapper;
import com.share.order.service.IOrderInfoService;


import com.share.rules.api.RemoteFeeRuleService;
import com.share.rules.domain.FeeRule;
import com.share.rules.domain.FeeRuleRequestForm;
import com.share.rules.domain.FeeRuleResponseVo;

import com.share.user.api.RemoteUserInfoService;
import com.share.user.domain.UserInfo;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service

    // 订单实体类：记录用户借还充电宝的完整信息，对应 order_info 表
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IOrderInfoService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private RemoteFeeRuleService remoteFeeRuleService;
    @Autowired
    private RemoteUserInfoService remoteUserInfoService;
    @Autowired
    private OrderBillMapper orderBillMapper;

    @Override

    // 查询用户未完成订单（充电中 status=0 或待支付 status=1）
    public OrderInfo getNoFinishOrder(Long userId) {
        // 查询用户是否有使用中与未支付订单
        return orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, userId)
                .in(OrderInfo::getStatus, Arrays.asList("0", "1"))// 订单状态：0:充电中 1：未支付 2：已支付
                .orderByDesc(OrderInfo::getId)
                .last("limit 1")
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @Override

    // 创建新订单：随机订单号 → 绑定充电宝 → 记录借用站点 → 设置费用规则 → 订单状态设为充电中
    public Long saveOrder(SubmitOrderVo orderForm) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserId(orderForm.getUserId());
        orderInfo.setOrderNo(RandomUtil.randomString(8));
        orderInfo.setPowerBankNo(orderForm.getPowerBankNo());
        orderInfo.setStartTime(new Date());
        orderInfo.setStartStationId(orderForm.getStartStationId());
        orderInfo.setStartStationName(orderForm.getStartStationName());
        orderInfo.setStartCabinetNo(orderForm.getStartCabinetNo());
        // 费用规则
        FeeRule feeRule = remoteFeeRuleService.getFeeRule(orderForm.getFeeRuleId()).getData();
        orderInfo.setFeeRuleId(orderForm.getFeeRuleId());
        orderInfo.setFeeRule(feeRule.getDescription());
        orderInfo.setStatus("0");
        orderInfo.setCreateTime(new Date());
        orderInfo.setCreateBy(SecurityUtils.getUsername());
        //用户昵称
        UserInfo userInfo = remoteUserInfoService.getInfo(orderInfo.getUserId()).getData();
        //orderInfo.setNickname(userInfo.getNickname());

        orderInfoMapper.insert(orderInfo);
        return orderInfo.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override

    // 结束订单（归还充电宝）：计算充电时长 → Drools 规则引擎计算费用 → 设置订单金额 → 插入账单明细 → 更新订单状态
    public void endOrder(EndOrderVo endOrderVo) {
        // 获取充电中的订单，如果存在，则结束订单； 如果不存在，则返回（初始化插入，无订单）
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getPowerBankNo, endOrderVo.getPowerBankNo())
                .eq(OrderInfo::getStatus, "0") //订单状态：0:充电中
                .orderByDesc(OrderInfo::getCreateTime)
                .last("limit 1")
        );
        if (orderInfo == null) {
            return;
        }

        orderInfo.setEndTime(endOrderVo.getEndTime());
        orderInfo.setEndStationId(endOrderVo.getEndStationId());
        orderInfo.setEndStationName(endOrderVo.getEndStationName());
        orderInfo.setEndCabinetNo(endOrderVo.getEndCabinetNo());
        int duration = Minutes.minutesBetween(new DateTime(orderInfo.getStartTime()), new DateTime(orderInfo.getEndTime())).getMinutes();
        orderInfo.setDuration(BigDecimal.valueOf(duration));

        // 费用计算
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDuration(duration);
        feeRuleRequestForm.setFeeRuleId(orderInfo.getFeeRuleId());

    // Drools 规则引擎计算订单费用：传人充电时长和规则ID → 加载规则 → 执行 → 返回计费结果
        R<FeeRuleResponseVo> feeRuleResponseVoResult = remoteFeeRuleService.calculateOrderFee(feeRuleRequestForm);
        if (R.FAIL == feeRuleResponseVoResult.getCode()) {
            throw new ServiceException(feeRuleResponseVoResult.getMsg());
        }
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

        // 设置订单金额（防御：totalAmount 为 null 时默认 0）
        BigDecimal totalAmount = feeRuleResponseVo.getTotalAmount();
        if (totalAmount == null) {
            totalAmount = BigDecimal.valueOf(0.0);
        }
        orderInfo.setTotalAmount(totalAmount);
        orderInfo.setDeductAmount(new BigDecimal(0));
        orderInfo.setRealAmount(totalAmount);
        if(orderInfo.getRealAmount().subtract(new BigDecimal(0)).doubleValue() == 0) {
            orderInfo.setStatus("2");
        } else {
            orderInfo.setStatus("1");
        }
        orderInfoMapper.updateById(orderInfo);

        // 插入免费订单账单
        OrderBill freeOrderBill = new OrderBill();
        freeOrderBill.setOrderId(orderInfo.getId());
        freeOrderBill.setBillItem(feeRuleResponseVo.getFreeDescription());
        freeOrderBill.setBillAmount(new BigDecimal(0));
        orderBillMapper.insert(freeOrderBill);

        // 插入超出免费订单账单
        if (feeRuleResponseVo.getExceedPrice().doubleValue() > 0) {
            OrderBill exceedOrderBill = new OrderBill();
            exceedOrderBill.setOrderId(orderInfo.getId());
            exceedOrderBill.setBillItem(feeRuleResponseVo.getExceedDescription());
            exceedOrderBill.setBillAmount(feeRuleResponseVo.getExceedPrice());
            orderBillMapper.insert(exceedOrderBill);
        }
    }


    @Override

    // 查询用户订单列表，充电中的订单实时计算使用时长和费用
    public List<OrderInfo> selectUserOrderInfoList(Long userId) {
        List<OrderInfo> orderInfoList = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, userId)
                .orderByDesc(OrderInfo::getId)
        );
        if (!CollectionUtils.isEmpty(orderInfoList)) {
            for (OrderInfo orderInfo : orderInfoList) {
                //充电中实时计算使用时间与金额
                calculateNowFee(orderInfo);
            }
        }
        return orderInfoList;
    }

    @Override

    // 查询订单详情，包含账单列表和用户信息
    public OrderInfo selectOrderInfoById(Long id) {
        OrderInfo orderInfo = orderInfoMapper.selectById(id);

        //充电中实时计算使用时间与金额
        calculateNowFee(orderInfo);

        List<OrderBill> orderBillList = orderBillMapper.selectList(new LambdaQueryWrapper<OrderBill>().eq(OrderBill::getOrderId, id));
        orderInfo.setOrderBillList(orderBillList);

        R<UserInfo> userInfoResult = remoteUserInfoService.getInfo(orderInfo.getUserId());
        if (StringUtils.isNull(userInfoResult) || StringUtils.isNull(userInfoResult.getData())) {
            throw new ServiceException("获取用户信息失败");
        }
        if (R.FAIL == userInfoResult.getCode()) {
            throw new ServiceException(userInfoResult.getMsg());
        }
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfoResult.getData(), userInfoVo);
        orderInfo.setUserInfoVo(userInfoVo);
        return orderInfo;
    }

    private void calculateNowFee(OrderInfo orderInfo) {
        if ("0".equals(orderInfo.getStatus())) {
            //充电中实时计算使用时间
            int duration = Minutes.minutesBetween(new DateTime(orderInfo.getStartTime()), new DateTime()).getMinutes();
            if (duration > 0) {
                orderInfo.setDuration(BigDecimal.valueOf(duration));

                // 费用计算
                FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
                feeRuleRequestForm.setDuration(duration);
                feeRuleRequestForm.setFeeRuleId(orderInfo.getFeeRuleId());
                R<FeeRuleResponseVo> feeRuleResponseVoResult = remoteFeeRuleService.calculateOrderFee(feeRuleRequestForm);
                if (R.FAIL == feeRuleResponseVoResult.getCode()) {
                    throw new ServiceException(feeRuleResponseVoResult.getMsg());
                }
                FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

                // 设置订单金额
                orderInfo.setTotalAmount(feeRuleResponseVo.getTotalAmount());
                orderInfo.setDeductAmount(new BigDecimal(0));
                orderInfo.setRealAmount(feeRuleResponseVo.getTotalAmount());
            } else {
                orderInfo.setDuration(BigDecimal.valueOf(0));
                orderInfo.setTotalAmount(new BigDecimal(0));
                orderInfo.setDeductAmount(new BigDecimal(0));
                orderInfo.setRealAmount(new BigDecimal(0));
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)

    // 模拟支付成功：通过 SQL 条件更新实现支付幂等，防止重复点击/重复回调导致重复入账
    public void simulatePaySuccess(String orderNo) {
        // 1. 条件更新：只有 status='1'(待支付) 的订单才能更新为已支付
        //    更新语句在数据库层是原子的，并发下重复支付只有一笔能命中，其余更新 0 行
        //    （等价于真实微信支付回调的处理：update ... where order_no=? and status=待支付）
        String transactionId = "SIMULATE_" + UUID.randomUUID().toString().replace("-", "");
        boolean updated = orderInfoMapper.update(
                null,
                new LambdaUpdateWrapper<OrderInfo>()
                        .eq(OrderInfo::getOrderNo, orderNo)
                        .eq(OrderInfo::getStatus, "1")
                        .set(OrderInfo::getStatus, "2")
                        .set(OrderInfo::getPayTime, new Date())
                        .set(OrderInfo::getTransactionId, transactionId)
        ) > 0;

        if (updated) {
            // 2. 首次支付成功
            return;
        }

        // 3. 更新 0 行说明订单当前不是"待支付"状态，需区分"已支付"与"非法状态"
        OrderInfo orderInfo = getByOrderNo(orderNo);
        if (orderInfo != null && "2".equals(orderInfo.getStatus())) {
            // 3.1 已支付 → 幂等返回成功（微信回调重试、用户重复点击"去支付"不会报错/重复扣款）
            return;
        }
        if (orderInfo == null) {
            throw new ServiceException("订单不存在");
        }
        // 3.2 其他非法状态（如已取消）
        throw new ServiceException("当前订单状态不允许支付");
    }

    @Override

    // 根据订单号精确查询订单
    public OrderInfo getByOrderNo(String orderNo) {
        return orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
    }

    @Override

    // 执行 AI 生成的 SQL 查询订单报表数据，动态识别列名适配前端图表
    public Map<String, Object> getOrderCount(String sql) {
        List<Map<String, Object>> list = baseMapper.getOrderCount(sql);

        Map dataMap = new HashMap<>();
        List<Object> dateList = new ArrayList<>();
        List<Object> countListVal = new ArrayList<>();

        if (!CollectionUtils.isEmpty(list)) {
            // 动态识别列名：第一列作为日期维度，第二列作为数值维度
            Map<String, Object> firstRow = list.get(0);
            String[] keys = firstRow.keySet().toArray(new String[0]);
            String dateKey = keys.length > 0 ? keys[0] : "order_date";
            String countKey = keys.length > 1 ? keys[1] : "order_count";

            for (Map<String, Object> row : list) {
                dateList.add(row.get(dateKey));
                countListVal.add(row.get(countKey));
            }
        }

        dataMap.put("dateList", dateList);
        dataMap.put("countList", countListVal);

        return dataMap;
    }

    @Override

    // 后台管理端查询订单列表，支持按订单号、充电宝编号、状态筛选
    public List<OrderInfo> selectOrderInfoList(OrderInfo orderInfo) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(orderInfo.getOrderNo())) {
            wrapper.like(OrderInfo::getOrderNo, orderInfo.getOrderNo());
        }
        if (StringUtils.isNotEmpty(orderInfo.getPowerBankNo())) {
            wrapper.like(OrderInfo::getPowerBankNo, orderInfo.getPowerBankNo());
        }
        if (StringUtils.isNotEmpty(orderInfo.getStatus())) {
            wrapper.eq(OrderInfo::getStatus, orderInfo.getStatus());
        }
        wrapper.orderByDesc(OrderInfo::getId);
        return baseMapper.selectList(wrapper);
    }
}
