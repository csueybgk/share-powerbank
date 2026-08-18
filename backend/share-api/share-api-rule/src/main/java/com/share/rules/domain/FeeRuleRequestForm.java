package com.share.rules.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data

    // 费用规则实体：Drools 规则名称、规则代码、状态
public class FeeRuleRequestForm {


    @Schema(description = "费用规则id")
    private Long FeeRuleId;

    @Schema(description = "借用时长")
    private Integer duration;

}