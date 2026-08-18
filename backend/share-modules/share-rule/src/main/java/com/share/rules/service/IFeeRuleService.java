package com.share.rules.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.share.rules.domain.FeeRule;
import com.share.rules.domain.FeeRuleRequestForm;
import com.share.rules.domain.FeeRuleResponseVo;

import java.util.List;

public interface IFeeRuleService extends IService<FeeRule> {



    // 查询费用规则列表
    public List<FeeRule> selectFeeRuleList(FeeRule feeRule);


    // 获取所有有效（status=1）的费用规则列表
    List<FeeRule> getALLFeeRuleList();


    // 按ID查询费用规则（带Redis缓存，热点数据，30分钟过期）
    FeeRule getFeeRuleByIdCache(Long id);


    // 按ID列表批量查询费用规则（带Redis缓存，逐条命中缓存，未命中的查库后回填）
    List<FeeRule> getFeeRuleListCache(List<Long> idList);


    // 删除费用规则的Redis缓存（修改/删除规则后调用，保证缓存一致性）
    void evictFeeRuleCache(Long id);


    // Drools 规则引擎计算订单费用：传人充电时长和规则ID → 加载规则 → 执行 → 返回计费结果
    FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm calculateOrderFeeForm);

}
