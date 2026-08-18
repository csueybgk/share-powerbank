package com.share.device.api;

import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.share.common.core.constant.DeviceConstants;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.RequiresLogin;
import com.share.device.domain.StationVo;
import com.share.device.service.IDeviceService;
import com.share.device.service.IStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.share.common.core.web.domain.AjaxResult.success;


@Tag(name = "站点接口管理")
@RestController
@RequestMapping("/device")
public class DeviceApiController {

    @Autowired
    private IDeviceService deviceService;
    @Autowired
    private IStationService stationService;

    @Operation(summary = "根据经纬度搜索附近门店（站点）")
    @RequiresLogin

    // 查询附近站点：MongoDB 地理位置查询 → 计算距离 → 组装可用充电宝数量、空闲卡槽数量、费用规则
    @GetMapping("/nearbyStation/{latitude}/{longitude}")
    public AjaxResult nearbyStation(@PathVariable String latitude, @PathVariable String longitude)
    {
        List<StationVo> stationVoList = deviceService.nearbyStation(latitude, longitude, DeviceConstants.SEARCH_H5_RADIUS);
        if (CollectionUtils.isEmpty(stationVoList)) {
            stationService.updateData();
        }
        return success(stationVoList);
    }

    @Operation(summary = "根据id获取门店详情")
    @RequiresLogin

    // 获取站点详情：包含距离计算、可借/可还状态、费用规则描述
    @GetMapping("/getStation/{id}/{latitude}/{longitude}")
    public AjaxResult getStation(@PathVariable Long id, @PathVariable String latitude, @PathVariable String longitude)
    {
        return success(deviceService.getStation(id, latitude, longitude));
    }

    @Operation(summary = "扫码充电")
    @RequiresLogin

    // 扫码充电核心方法：免押金校验 → 未归还检查 → 选充电宝 → 锁定卡槽 → MQTT 下发开锁指令
    @GetMapping("scanCharge/{cabinetNo}")
    public AjaxResult scanCharge(@PathVariable String cabinetNo) {
        return success(deviceService.scanCharge(cabinetNo));
    }
}
