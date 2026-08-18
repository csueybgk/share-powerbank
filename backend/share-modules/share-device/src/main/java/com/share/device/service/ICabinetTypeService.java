package com.share.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.device.domain.CabinetType;

import java.util.List;

public interface ICabinetTypeService extends IService<CabinetType> {
    //分页查询
    List<CabinetType> selectCabinetTypeList(CabinetType cabinetType);
}
