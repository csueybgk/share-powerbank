package com.share.rules.api;

import com.share.common.core.domain.R;

import com.share.rules.domain.FeeRule;
import com.share.rules.domain.FeeRuleRequestForm;
import com.share.rules.domain.FeeRuleResponseVo;
import com.share.rules.service.IFeeRuleService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/feeRule")
@SuppressWarnings({"unchecked", "rawtypes"})

    // 费用规则实体：Drools 规则名称、规则代码、状态
public class FeeRuleApiController {

    @Autowired
    private IFeeRuleService feeRuleService;

    @Operation(summary = "批量获取费用规则信息")
    @PostMapping(value = "/getFeeRuleList")
    public R<List<FeeRule>> getFeeRuleList(@RequestBody List<Long> feeRuleIdList)
    {
        // 带Redis缓存：附近站点等高频场景批量读取，未命中的查库回填
        return R.ok(feeRuleService.getFeeRuleListCache(feeRuleIdList));
    }

    @Operation(summary = "获取费用规则详细信息")
    @GetMapping(value = "/getFeeRule/{id}")
    public R<FeeRule> getFeeRule(@PathVariable("id") Long id)
    {
        // 带Redis缓存：订单创建、费用计算等高频场景读取
        return R.ok(feeRuleService.getFeeRuleByIdCache(id));
    }

    @Operation(summary = "计算订单费用")

    // Drools 规则引擎计算订单费用：传人充电时长和规则ID → 加载规则 → 执行 → 返回计费结果
    @PostMapping("/calculateOrderFee")
    public R<FeeRuleResponseVo> calculateOrderFee(@RequestBody FeeRuleRequestForm calculateOrderFeeForm) {
        return R.ok(feeRuleService.calculateOrderFee(calculateOrderFeeForm));
    }


}
