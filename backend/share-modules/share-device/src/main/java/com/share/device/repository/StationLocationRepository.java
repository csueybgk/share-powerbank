package com.share.device.repository;

import com.share.device.domain.StationLocation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StationLocationRepository extends MongoRepository<StationLocation, String> {

    // 根据站点ID查询站点位置信息
    StationLocation getByStationId(Long stationId);
    // 根据站点ID删除站点位置信息
    void deleteByStationId(Long stationId);
}
