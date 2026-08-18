package com.share.device.service;

import com.share.device.domain.CabinetSlot;
import com.share.device.domain.ScanChargeVo;
import com.share.device.domain.StationVo;

import java.util.List;

public interface IDeviceService
{


    // 查询附近站点：MongoDB 地理位置查询 → 计算距离 → 组装可用充电宝数量、空闲卡槽数量、费用规则
    List<StationVo> nearbyStation(String latitude, String longitude, Integer radius);


    // 获取站点详情：包含距离计算、可借/可还状态、费用规则描述
    StationVo getStation(Long id, String latitude, String longitude);


    // 扫码充电核心方法：免押金校验 → 未归还检查 → 选充电宝 → 锁定卡槽 → MQTT 下发开锁指令
    ScanChargeVo scanCharge(String cabinetNo);


    // 解锁卡槽：延迟消息回调，若卡槽仍为锁定状态则恢复为空闲
    void unlockSlot(CabinetSlot cabinetSlot);
}
