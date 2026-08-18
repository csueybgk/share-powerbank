package com.share.device.service.impl;


import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.context.SecurityContextHolder;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.common.rabbit.constant.MqConst;
import com.share.common.rabbit.service.RabbitService;
import com.share.common.security.utils.SecurityUtils;
import com.share.device.domain.*;

import com.share.device.emqx.EmqxClientWrapper;

import com.share.device.emqx.constant.EmqxConstants;
import com.share.device.service.*;

import com.share.order.api.RemoteOrderInfoService;
import com.share.order.domain.OrderInfo;

import com.share.rules.api.RemoteFeeRuleService;
import com.share.rules.domain.FeeRule;

import com.share.user.api.RemoteUserInfoService;
import com.share.user.domain.UserInfo;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoTemplate;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DeviceServiceImpl implements IDeviceService {

    @Autowired
    private IStationService stationService;

    @Autowired
    private ICabinetService cabinetService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RemoteFeeRuleService remoteFeeRuleService;

    @Autowired
    private IMapService mapService;

    @Autowired
    private RemoteOrderInfoService remoteOrderInfoService;

    @Autowired
    private RemoteUserInfoService remoteUserInfoService;

    @Autowired
    private EmqxClientWrapper emqxClientWrapper;

    @Autowired
    private ICabinetSlotService cabinetSlotService;

    @Autowired
    private IPowerBankService powerBankService;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override

    // 查询附近站点：MongoDB 地理位置查询 → 计算距离 → 组装可用充电宝数量、空闲卡槽数量、费用规则
    public List<StationVo> nearbyStation(String latitude, String longitude, Integer radius) {
        //坐标，确定中心点
        // GeoJsonPoint(double x, double y) x 表示经度，y 表示纬度。
        GeoJsonPoint geoJsonPoint = new GeoJsonPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));
        //画圈的半径,50km范围
        Distance d = new Distance(radius, Metrics.KILOMETERS);
        //画了一个圆圈
        Circle circle = new Circle(geoJsonPoint, d);
        //条件排除自己
        Query query = Query.query(Criteria.where("location").withinSphere(circle));
        List<StationLocation> stationLocationList = this.mongoTemplate.find(query, StationLocation.class);
        if (CollectionUtils.isEmpty(stationLocationList)) return null;

        //组装数据

    // 获取站点详情：包含距离计算、可借/可还状态、费用规则描述
        List<Long> stationIdList =stationLocationList.stream().map(StationLocation::getStationId).collect(Collectors.toList());
        //获取站点列表
        List<Station> stationList = stationService.list(new LambdaQueryWrapper<Station>().in(Station::getId, stationIdList).isNotNull(Station::getCabinetId));

        //获取柜机id列表
        List<Long> cabinetIdList = stationList.stream().map(Station::getCabinetId).collect(Collectors.toList());
        //获取柜机id与柜机信息Map
        Map<Long, Cabinet> cabinetIdToCabinetMap = cabinetService.listByIds(cabinetIdList).stream().collect(Collectors.toMap(Cabinet::getId, Cabinet -> Cabinet));

        //获取柜机id列表
        List<Long> feeRuleIdList = stationList.stream().map(Station::getFeeRuleId).collect(Collectors.toList());
        //获取柜机id与柜机信息Map
        Map<Long, FeeRule> feeRuleIdToFeeRuleMap = remoteFeeRuleService.getFeeRuleList(feeRuleIdList).getData().stream().collect(Collectors.toMap(FeeRule::getId, FeeRule -> FeeRule));

        List<StationVo> stationVoList = new ArrayList<>();
        stationList.forEach(item -> {
            StationVo stationVo = new StationVo();
            BeanUtils.copyProperties(item, stationVo);
            // 计算距离
            Double distance = mapService.calculateDistance(longitude, latitude, item.getLongitude().toString(), item.getLatitude().toString());
            stationVo.setDistance(distance);

            // 获取柜机信息
            Cabinet cabinet = cabinetIdToCabinetMap.get(item.getCabinetId());
            //可用充电宝数量大于0，可借用
            if(cabinet.getAvailableNum() > 0) {
                stationVo.setIsUsable("1");
            } else {
                stationVo.setIsUsable("0");
            }
            // 获取空闲插槽数量大于0，可归还
            if (cabinet.getFreeSlots() > 0) {
                stationVo.setIsReturn("1");
            } else {
                stationVo.setIsReturn("0");
            }

            // 获取费用规则
            FeeRule feeRule = feeRuleIdToFeeRuleMap.get(item.getFeeRuleId());
            stationVo.setFeeRule(feeRule.getDescription());

            stationVoList.add(stationVo);
        });
        return stationVoList;
    }


    @Override
    public StationVo getStation(Long id, String latitude, String longitude) {
        Station station = stationService.getById(id);
        StationVo stationVo = new StationVo();
        BeanUtils.copyProperties(station, stationVo);
        // 计算距离
        Double distance = mapService.calculateDistance(longitude, latitude, station.getLongitude().toString(), station.getLatitude().toString());
        stationVo.setDistance(distance);

        // 获取柜机信息
        Cabinet cabinet = cabinetService.getById(station.getCabinetId());
        //可用充电宝数量大于0，可借用
        if(cabinet.getAvailableNum() > 0) {
            stationVo.setIsUsable("1");
        } else {
            stationVo.setIsUsable("0");
        }
        // 获取空闲插槽数量大于0，可归还
        if (cabinet.getFreeSlots() > 0) {
            stationVo.setIsReturn("1");
        } else {
            stationVo.setIsReturn("0");
        }

        // 获取费用规则
        FeeRule feeRule = remoteFeeRuleService.getFeeRule(station.getFeeRuleId()).getData();
        stationVo.setFeeRule(feeRule.getDescription());
        return stationVo;
    }


    @Override

    // 扫码充电核心方法：免押金校验 → 未归还检查 → 选充电宝 → 锁定卡槽 → MQTT 下发开锁指令
    public ScanChargeVo scanCharge(String cabinetNo) {
        // 扫码充电返回对象
        ScanChargeVo scanChargeVo = new ScanChargeVo();

        //免押金判断
        R<UserInfo> userInfoResult =  remoteUserInfoService.getInfo(SecurityContextHolder.getUserId());
        if (R.FAIL == userInfoResult.getCode()) {
            throw new ServiceException(userInfoResult.getMsg());
        }
        UserInfo userInfo = userInfoResult.getData();
        if (null == userInfo) {
            throw new ServiceException("获取用户信息失败");
        }
        if("0".equals(userInfo.getDepositStatus())) {
            throw new ServiceException("未申请免押金使用");
        }


    // 查询用户未完成订单（充电中 status=0 或待支付 status=1）
        R<OrderInfo> orderInfoResult = remoteOrderInfoService.getNoFinishOrder(SecurityUtils.getUserId());
        if (R.FAIL == orderInfoResult.getCode()) {
            throw new ServiceException(orderInfoResult.getMsg());
        }
        OrderInfo orderInfo = orderInfoResult.getData();
        if(null != orderInfo) {
            if("0".equals(orderInfo.getStatus())) {
                scanChargeVo.setStatus("2");
                scanChargeVo.setMessage("有未归还充电宝，请归还后使用");
                return scanChargeVo;
            }
            if("1".equals(orderInfo.getStatus())) {
                scanChargeVo.setStatus("3");
                scanChargeVo.setMessage("有未支付订单，去支付");
                return scanChargeVo;
            }
        }

        // 获取可用充电宝信息

    // 根据柜机编号选出电量最高的可用充电宝并锁定卡槽，使用 Redis 分布式锁 + SQL 条件更新双重防并发
        AvailableProwerBankVo availableProwerBankVo = this.checkAvailableProwerBank(cabinetNo);
        if(null == availableProwerBankVo) {
            throw new ServiceException("无可用充电宝");
        }
        if(!StringUtils.isEmpty(availableProwerBankVo.getErrMessage())) {
            throw new ServiceException(availableProwerBankVo.getErrMessage());
        }

        // 生成借取指令，弹出充电宝
        JSONObject object = new JSONObject();
        object.put("uId", SecurityContextHolder.getUserId());
        object.put("mNo", "mm"+ RandomUtil.randomString(8));
        object.put("cNo", cabinetNo);
        object.put("pNo", availableProwerBankVo.getPowerBankNo());
        object.put("sNo", availableProwerBankVo.getSlotNo());
        String topic = String.format(EmqxConstants.TOPIC_SCAN_SUBMIT, cabinetNo);
        emqxClientWrapper.publish(topic, object.toJSONString());

        scanChargeVo.setStatus("1");
        return scanChargeVo;
    }

    /**
     * 根据柜机编号获取一个可用最优的充电宝
     * @param cabinetNo
     * @return
     */
    public AvailableProwerBankVo checkAvailableProwerBank(String cabinetNo) {
        AvailableProwerBankVo availableProwerBankVo = new AvailableProwerBankVo();

        // ========== Redisson 分布式锁：防止并发扫码同一柜机 ==========
        String lockKey = "cabinet:charge:lock:" + cabinetNo;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // tryLock(等待时间, 持有时间, 单位)：不等待，获取失败直接返回 false
            boolean locked = lock.tryLock(0, 5, TimeUnit.SECONDS);
            if (!locked) {
                throw new ServiceException("当前使用人数较多，请稍后再试");
            }
        // ========== 锁保护区域 ==========

        Cabinet cabinet = cabinetService.getOne(new LambdaQueryWrapper<Cabinet>().eq(Cabinet::getCabinetNo, cabinetNo));
        if(cabinet.getAvailableNum() == 0) {
            availableProwerBankVo.setErrMessage("无可用充电宝");
            return availableProwerBankVo;
        }
        // 获取插槽列表
        List<CabinetSlot> cabinetSlotList = cabinetSlotService.list(new LambdaQueryWrapper<CabinetSlot>()
                .eq(CabinetSlot::getCabinetId, cabinet.getId())
                .eq(CabinetSlot::getStatus, "1") // 状态（1：占用 0：空闲 2：锁定）
        );
        // 获取插槽对应的充电宝id列表
        List<Long> powerBankIdList = cabinetSlotList.stream().filter(item -> null != item.getPowerBankId()).map(CabinetSlot::getPowerBankId).collect(Collectors.toList());
        //获取可用充电宝列表
        List<PowerBank> powerBankList = powerBankService.list(new LambdaQueryWrapper<PowerBank>().in(PowerBank::getId, powerBankIdList).eq(PowerBank::getStatus, "1"));
        if(CollectionUtils.isEmpty(powerBankList)) {
            availableProwerBankVo.setErrMessage("无可用充电宝");
            return availableProwerBankVo;
        }
        // 根据电量降序排列
        if(powerBankList.size() > 1) {
            Collections.sort(powerBankList, (o1, o2) -> o2.getElectricity().compareTo(o1.getElectricity()));
        }
        // 获取电量最多的充电宝
        PowerBank powerBank = powerBankList.get(0);
        // 获取电量最多的充电宝插槽信息
        CabinetSlot cabinetSlot = cabinetSlotList.stream().filter(item -> null != item.getPowerBankId() && item.getPowerBankId().equals(powerBank.getId())).collect(Collectors.toList()).get(0);
        //锁定柜机卡槽（条件更新：只有 status='1' 时才能锁定，防止并发覆盖）
        boolean updated = cabinetSlotService.update(
            new UpdateWrapper<CabinetSlot>()
                .eq("id", cabinetSlot.getId())
                .eq("status", "1")
                .set("status", "2")
        );
        if (!updated) {
            throw new ServiceException("该充电宝已被他人占用，请重试");
        }

        // 设置返回对象
        availableProwerBankVo.setPowerBankNo(powerBank.getPowerBankNo());
        availableProwerBankVo.setSlotNo(cabinetSlot.getSlotNo());

        // 扫码后未弹出充电宝等情况，延迟解锁
        rabbitService.sendDealyMessage(MqConst.EXCHANGE_DEVICE, MqConst.ROUTING_UNLOCK_SLOT, JSONObject.toJSONString(cabinetSlot), MqConst.CANCEL_UNLOCK_SLOT_DELAY_TIME);

        // ========== 锁保护区域结束 ==========
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("系统繁忙，请稍后再试");
        } finally {
            // Redisson 自动校验锁归属，只有持有者才能释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return availableProwerBankVo;
    }

    @Override

    // 解锁卡槽：延迟消息回调，若卡槽仍为锁定状态则恢复为空闲
    public void unlockSlot(CabinetSlot cs) {
        CabinetSlot cabinetSlot = cabinetSlotService.getById(cs.getId());
        if("2".equals(cabinetSlot.getStatus())) {
            //状态（1：占用 0：空闲 2：锁定）
            cabinetSlot.setStatus("1");
            cabinetSlot.setUpdateTime(new Date());
            cabinetSlotService.updateById(cabinetSlot);
        }
    }
}
