package com.share.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.device.domain.Cabinet;

import java.util.List;
import java.util.Map;

public interface ICabinetService extends IService<Cabinet>
{

    List<Cabinet> selectCabinetList(Cabinet cabinet);


    List<Cabinet> searchNoUseList(String keyword);

    Map<String, Object> getAllInfo(Long id);

    Cabinet getBtCabinetNo(String cabinetNo);
}