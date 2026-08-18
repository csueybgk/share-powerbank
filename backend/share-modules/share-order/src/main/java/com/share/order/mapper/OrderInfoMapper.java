package com.share.order.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.order.domain.OrderInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {


    // 执行 AI 生成的 SQL 查询订单报表数据，动态识别列名适配前端图表
    List<Map<String, Object>> getOrderCount(String sql);
}
