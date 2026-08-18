package com.share.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.device.domain.PowerBank;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PowerBankMapper extends BaseMapper<PowerBank>
{

    /**
     * 查询充电宝列表
     *
     * @param powerBank 充电宝
     * @return 充电宝集合
     */
    List<PowerBank> selectPowerBankList(PowerBank powerBank);

}
