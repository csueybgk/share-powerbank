package com.share.rules.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.rules.domain.FeeRule;
import com.share.rules.service.IFeeRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Tag(name = "费用规则接口管理")
@RestController
@RequestMapping("/feeRule")

    // 费用规则管理 CRUD：支持动态修改 Drools 规则代码后实时生效
public class FeeRuleController extends BaseController
{
    @Autowired
    private IFeeRuleService feeRuleService;

    /**
     * 查询费用规则列表
     */
    @Operation(summary = "查询费用规则列表")
    @GetMapping("/list")
    public TableDataInfo list(FeeRule feeRule)
    {
        startPage();

    // 查询费用规则列表
        List<FeeRule> list = feeRuleService.selectFeeRuleList(feeRule);
        return getDataTable(list);
    }

    /**
     * 获取费用规则详细信息
     */
    @Operation(summary = "获取费用规则详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        // 带Redis缓存读取
        return success(feeRuleService.getFeeRuleByIdCache(id));
    }

    /**
     * 新增费用规则
     */
    @Operation(summary = "新增费用规则")
    @PostMapping
    public AjaxResult add(@RequestBody FeeRule feeRule)
    {
        return toAjax(feeRuleService.save(feeRule));
    }

    /**
     * 修改费用规则
     */
    @Operation(summary = "修改费用规则")
    @PutMapping
    public AjaxResult edit(@RequestBody FeeRule feeRule)
    {
        boolean updated = feeRuleService.updateById(feeRule);
        if (updated && feeRule.getId() != null) {
            // 规则已修改 → 删除Redis缓存，下次读取回源数据库（缓存一致性）
            feeRuleService.evictFeeRuleCache(feeRule.getId());
        }
        return toAjax(updated ? 1 : 0);
    }

    /**
     * 删除费用规则
     */
    @Operation(summary = "删除费用规则")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        List<Long> idList = Arrays.asList(ids);
        // 先删缓存再删库（即使删除失败，缓存也会按过期时间兜底）
        for (Long id : idList) {
            feeRuleService.evictFeeRuleCache(id);
        }
        return toAjax(feeRuleService.removeBatchByIds(idList));
    }

    @Operation(summary = "获取全部费用规则")

    // 获取所有有效（status=1）的费用规则列表
    @GetMapping("/getALLFeeRuleList")
    public AjaxResult getALLFeeRuleList()
    {
        return success(feeRuleService.getALLFeeRuleList());
    }

}
