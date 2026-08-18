package com.share.rules.api;

import com.share.common.core.constant.ServiceNameConstants;
import com.share.common.core.domain.R;


import com.share.rules.domain.FeeRule;
import com.share.rules.domain.FeeRuleRequestForm;
import com.share.rules.domain.FeeRuleResponseVo;
import com.share.rules.factory.RemoteFeeRuleFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务
 *
 */
@FeignClient(contextId = "remoteFeeRuleService", value = ServiceNameConstants.RULE_SERVICE, fallbackFactory = RemoteFeeRuleFallbackFactory.class)

    // 远程费用规则服务 Feign 接口：获取规则列表、计算订单费用
public interface RemoteFeeRuleService {

    @PostMapping(value = "/feeRule/getFeeRuleList")
    public R<List<FeeRule>> getFeeRuleList(@RequestBody List<Long> feeRuleIdList);

    @GetMapping(value = "/feeRule/getFeeRule/{id}")
    public R<FeeRule> getFeeRule(@PathVariable("id") Long id);


    // Drools 规则引擎计算订单费用：传人充电时长和规则ID → 加载规则 → 执行 → 返回计费结果
    @PostMapping("/feeRule/calculateOrderFee")
    public R<FeeRuleResponseVo> calculateOrderFee(@RequestBody FeeRuleRequestForm feeRuleRequestForm);

}
