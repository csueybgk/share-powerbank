package com.share.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.share.device.domain.Cabinet;
import com.share.device.domain.Station;
import com.share.device.domain.StationLocation;
import com.share.device.mapper.StationMapper;
import com.share.device.repository.StationLocationRepository;
import com.share.device.service.ICabinetService;
import com.share.device.service.IRegionService;
import com.share.device.service.IStationService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StationServiceImpl extends ServiceImpl<StationMapper, Station> implements IStationService
{
    @Autowired
    private StationMapper stationMapper;
    @Autowired
    private ICabinetService cabinetService;
    @Autowired
    private IRegionService regionService;
    @Autowired
    private StationLocationRepository stationLocationRepository;

    @Override
    public List<Station> selectStationList(Station station)
    {
        // 第1步：查询站点列表（带分页）
        List<Station> list = stationMapper.selectStationList(station);
        // 第2步：提取所有站点的柜机ID
        List<Long> cabinetIdList = list.stream()
                .map(Station::getCabinetId)
                .collect(Collectors.toList());
        // 第3步：准备柜机ID到柜机编号的映射
        Map<Long,String> cabinetIdToCabinetNoMap = new HashMap<>();
        // 第4步：批量查询柜机信息并构建映射关系
        if(!CollectionUtils.isEmpty(cabinetIdList)) {
            List<Cabinet> cabinetList = cabinetService.list(new LambdaQueryWrapper<Cabinet>().in(Cabinet::getId, cabinetIdList));
            // 将柜机列表转换为 Map<ID, 柜机编号>
            cabinetIdToCabinetNoMap = cabinetList.stream().collect(Collectors.toMap(Cabinet::getId, Cabinet::getCabinetNo));
        }
        // 第5步：为每个站点填充柜机编号
        for (Station item : list) {
            item.setCabinetNo(cabinetIdToCabinetNoMap.get(item.getCabinetId()));
        }
        return list;
    }



    @Transactional(rollbackFor = Exception.class)
    @Override
    public int saveStation(Station station) {
        String provinceName = regionService.getNameByCode(station.getProvinceCode());
        String cityName = regionService.getNameByCode(station.getCityCode());
        String districtName = regionService.getNameByCode(station.getDistrictCode());
        station.setFullAddress(provinceName + cityName + districtName + station.getAddress());
        this.save(station);

        //同步站点位置信息到MongoDB
        StationLocation stationLocation = new StationLocation();
        stationLocation.setId(ObjectId.get().toString());
        stationLocation.setStationId(station.getId());
        stationLocation.setLocation(new GeoJsonPoint(station.getLongitude().doubleValue(), station.getLatitude().doubleValue()));
        stationLocation.setCreateTime(new Date());
        stationLocationRepository.save(stationLocation);

        return 1;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateStation(Station station) {
        String provinceName = regionService.getNameByCode(station.getProvinceCode());
        String cityName = regionService.getNameByCode(station.getCityCode());
        String districtName = regionService.getNameByCode(station.getDistrictCode());
        station.setFullAddress(provinceName + cityName + districtName + station.getAddress());
        this.updateById(station);

        //同步站点位置信息到MongoDB
        StationLocation stationLocation = stationLocationRepository.getByStationId(station.getId());
        stationLocation.setLocation(new GeoJsonPoint(station.getLongitude().doubleValue(), station.getLatitude().doubleValue()));
        stationLocationRepository.save(stationLocation);

        return 1;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeByIds(Collection<?> list) {
        for (Object id : list) {
            stationLocationRepository.deleteByStationId(Long.parseLong(id.toString()));
        }
        return super.removeByIds(list);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setData(Station station) {
        this.updateById(station);

        //更正柜机使用状态
        Cabinet cabinet = cabinetService.getById(station.getCabinetId());
        cabinet.setStatus("1");
        cabinetService.updateById(cabinet);
        return 1;
    }
    @Override
    public void updateData() {
        List<Station> stationList = this.list();
        for (Station station : stationList) {
            StationLocation stationLocation = stationLocationRepository.getByStationId(station.getId());
            if(stationLocation == null) {
                stationLocation = new StationLocation();
                stationLocation.setId(ObjectId.get().toString());
                stationLocation.setStationId(station.getId());
                stationLocation.setLocation(new GeoJsonPoint(station.getLongitude().doubleValue(), station.getLatitude().doubleValue()));
                stationLocation.setCreateTime(new Date());
                stationLocationRepository.save(stationLocation);
            }
        }
    }

    @Override
    public Station getByCabinetId(Long cabinetId) {
        return stationMapper.selectOne(new LambdaQueryWrapper<Station>().eq(Station::getCabinetId, cabinetId));
    }
}
