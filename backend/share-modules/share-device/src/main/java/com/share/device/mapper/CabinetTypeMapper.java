package com.share.device.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.device.domain.CabinetType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CabinetTypeMapper extends BaseMapper<CabinetType> {
    // 分页查询
    List<CabinetType> selectCabinetTypeList(CabinetType cabinetType);
}
