package com.share.rules.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


    // Drools 工具类：根据规则文本字符串加载 KieSession
import com.share.common.redis.service.RedisService;
import com.share.rules.config.DroolsHelper;
import com.share.rules.domain.*;
import com.share.rules.mapper.FeeRuleMapper;
import com.share.rules.service.IFeeRuleService;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service

    // 费用规则实体：Drools 规则名称、规则代码、状态
public class FeeRuleServiceImpl extends ServiceImpl<FeeRuleMapper, FeeRule> implements IFeeRuleService
{
    @Autowired
    private FeeRuleMapper feeRuleMapper;

    @Autowired
    private KieContainer kieContainer;

    @Autowired
    private RedisService redisService;

    /** 费用规则缓存 key 前缀 */
    private static final String FEE_RULE_CACHE_KEY_PREFIX = "share:feeRule:";

    /** 费用规则缓存过期时间（秒）：30分钟 */
    private static final long FEE_RULE_CACHE_EXPIRE = 30 * 60L;

    /** 缓存过期时间随机抖动上限（秒）：避免大量 key 同一时刻过期引发缓存雪崩 */
    private static final long FEE_RULE_CACHE_EXPIRE_JITTER = 5 * 60L;

    /** 缓存击穿重建互斥锁 key 后缀 */
    private static final String FEE_RULE_CACHE_LOCK_SUFFIX = ":lock";

    /** 缓存穿透空值标记：不存在的 id 缓存空字符串 */
    private static final String NULL_CACHE_MARKER = "";

    /** 缓存穿透空值标记过期时间（秒）：3分钟 */
    private static final long NULL_CACHE_EXPIRE = 3 * 60L;



    @Override

    // 查询费用规则列表
    public List<FeeRule> selectFeeRuleList(FeeRule feeRule)
    {
        return feeRuleMapper.selectFeeRuleList(feeRule);
    }

    @Override

    // 获取所有有效（status=1）的费用规则列表
    public List<FeeRule> getALLFeeRuleList() {
        return feeRuleMapper.selectList(new LambdaQueryWrapper<FeeRule>().eq(FeeRule::getStatus, "1"));
    }

    /**
     * 组装费用规则缓存 key
     */
    private String feeRuleCacheKey(Long id) {
        return FEE_RULE_CACHE_KEY_PREFIX + id;
    }

    /** 从缓存读取费用规则；返回 null 表示无有效值（未命中或命中空值标记） */
    private FeeRule getCachedFeeRule(String key) {
        Object obj = redisService.getCacheObject(key);
        if (obj == null) {
            return null;
        }
        // 空值标记（缓存穿透防护缓存的是空字符串）
        if (obj instanceof String && ((String) obj).isEmpty()) {
            return null;
        }
        return (FeeRule) obj;
    }

    /** 缓存 key 是否存在（含空值标记） */
    private boolean hasCache(String key) {
        return Boolean.TRUE.equals(redisService.hasKey(key));
    }

    /** 获取带随机抖动的过期时间（秒）：避免大量 key 同时过期引发缓存雪崩 */
    private long getCacheExpireWithJitter() {
        return FEE_RULE_CACHE_EXPIRE + ThreadLocalRandom.current().nextLong(0, FEE_RULE_CACHE_EXPIRE_JITTER + 1);
    }

    @Override

    // 按ID查询费用规则（带Redis缓存），三重防护：
    // 1) 缓存穿透：不存在的 id 缓存 3 分钟空值标记，无效查询不再反复打库
    // 2) 缓存击穿：缓存重建用 SETNX 互斥锁 + 双检，热点 key 过期瞬间只有一个线程查库
    // 3) 缓存雪崩：过期时间加随机抖动，避免大量 key 同一时刻过期
    public FeeRule getFeeRuleByIdCache(Long id) {
        if (id == null) {
            return null;
        }
        String key = feeRuleCacheKey(id);

        // 1. 读缓存：命中有值或空值标记都直接返回（空值标记返回 null）
        FeeRule cached = getCachedFeeRule(key);
        if (cached != null || hasCache(key)) {
            return cached;
        }

        // 2. 缓存击穿防护：SETNX 分布式锁，只允许一个线程重建缓存，其余线程等待
        String lockKey = key + FEE_RULE_CACHE_LOCK_SUFFIX;
        boolean locked = redisService.setCacheObjectIfAbsent(lockKey, "1", 5L, TimeUnit.SECONDS);
        if (locked) {
            try {
                // 双检：等待锁期间可能已被其他线程重建
                cached = getCachedFeeRule(key);
                if (cached != null || hasCache(key)) {
                    return cached;
                }
                FeeRule feeRule = feeRuleMapper.selectById(id);
                if (feeRule != null) {
                    // 回填真实数据（带抖动 TTL）
                    redisService.setCacheObject(key, feeRule, getCacheExpireWithJitter(), TimeUnit.SECONDS);
                } else {
                    // 缓存穿透防护：不存在的 id 缓存空值标记（短TTL），无效查询不再反复打库
                    redisService.setCacheObject(key, NULL_CACHE_MARKER, NULL_CACHE_EXPIRE, TimeUnit.SECONDS);
                }
                return feeRule;
            } finally {
                // 释放锁（互斥锁有 5 秒过期兜底，防止持锁线程异常导致死锁）
                redisService.deleteObject(lockKey);
            }
        }

        // 3. 未抢到锁：自旋等待其他线程重建完成（最多约 500ms），超时兜底直接查库
        for (int i = 0; i < 10; i++) {
            cached = getCachedFeeRule(key);
            if (cached != null || hasCache(key)) {
                return cached;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return feeRuleMapper.selectById(id);
    }

    @Override

    // 批量查询费用规则（带Redis缓存）：逐条命中缓存，未命中的批量查库后逐条回填
    public List<FeeRule> getFeeRuleListCache(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return new ArrayList<>();
        }
        List<FeeRule> result = new ArrayList<>();
        List<Long> missIdList = new ArrayList<>();
        // 1. 逐条从缓存取，记录未命中的 id
        for (Long id : idList) {
            FeeRule cached = getFeeRuleByIdCache(id);
            if (cached != null) {
                result.add(cached);
            } else {
                missIdList.add(id);
            }
        }
        // 2. 未命中的批量查库
        if (!missIdList.isEmpty()) {
            Map<Long, FeeRule> dbMap = feeRuleMapper.selectBatchIds(missIdList).stream()
                    .collect(Collectors.toMap(FeeRule::getId, feeRule -> feeRule));
            for (Long id : missIdList) {
                FeeRule feeRule = dbMap.get(id);
                if (feeRule != null) {
                    // 3. 回填缓存
                    redisService.setCacheObject(feeRuleCacheKey(id), feeRule, FEE_RULE_CACHE_EXPIRE, TimeUnit.SECONDS);
                    result.add(feeRule);
                }
            }
        }
        return result;
    }

    @Override

    // 删除费用规则缓存（修改/删除规则后调用，保证缓存一致性）
    public void evictFeeRuleCache(Long id) {
        if (id != null) {
            redisService.deleteObject(feeRuleCacheKey(id));
        }
    }

    @Override

    // Drools 规则引擎计算订单费用：传人充电时长和规则ID → 加载规则 → 执行 → 返回计费结果
    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm feeRuleRequestForm) {
        //封装传入对象
        FeeRuleRequest feeRuleRequest = new FeeRuleRequest();
        feeRuleRequest.setDurations(feeRuleRequestForm.getDuration());
        log.info("传入参数：{}", JSON.toJSONString(feeRuleRequest));

        // 获取最新订单费用规则（带Redis缓存：实时计费为最热点读，避免每次计算都查库）
        FeeRule feeRule = getFeeRuleByIdCache(feeRuleRequestForm.getFeeRuleId());

    // 动态加载 Drools 规则：将字符串规则编译为 KieSession，用于运行时热更新计费规则
        KieSession kieSession = DroolsHelper.loadForRule(feeRule.getRule());

        //封装返回对象
        FeeRuleResponse feeRuleResponse = new FeeRuleResponse();
        kieSession.setGlobal("feeRuleResponse", feeRuleResponse);
        // 设置订单对象
        kieSession.insert(feeRuleRequest);
        // 触发规则
        kieSession.fireAllRules();
        // 中止会话
        kieSession.dispose();
        log.info("计算结果：{}", JSON.toJSONString(feeRuleResponse));

        //封装返回对象
        FeeRuleResponseVo feeRuleResponseVo = new FeeRuleResponseVo();
        feeRuleResponseVo.setTotalAmount(new BigDecimal(feeRuleResponse.getTotalAmount()));
        feeRuleResponseVo.setFreePrice(new BigDecimal(feeRuleResponse.getFreePrice()));
        feeRuleResponseVo.setExceedPrice(new BigDecimal(feeRuleResponse.getExceedPrice()));
        feeRuleResponseVo.setFreeDescription(feeRuleResponse.getFreeDescription());
        feeRuleResponseVo.setExceedDescription(feeRuleResponse.getExceedDescription());
        return feeRuleResponseVo;
    }
}
