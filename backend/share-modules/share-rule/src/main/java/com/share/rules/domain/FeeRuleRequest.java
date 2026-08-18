package com.share.rules.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data

    // 费用规则实体：Drools 规则名称、规则代码、状态
public class FeeRuleRequest {

    @Schema(description = "借用时长")
    private Integer durations;

    @Schema(description = "超出免费时长的小时数")
    private Integer exceedHours;
}