package com.share.rules.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.rules.domain.FeeRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FeeRuleMapper extends BaseMapper<FeeRule> {


    // 查询费用规则列表
    public List<FeeRule> selectFeeRuleList(FeeRule feeRule);

}
