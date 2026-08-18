package com.share.device.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.device.domain.CabinetType;
import com.share.device.service.ICabinetTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Tag(name = "柜机类型接口管理")
@RestController
@RequestMapping("/cabinetType")
public class CabinetTypeController extends BaseController {

    @Autowired
    private ICabinetTypeService cabinetTypeService;

    /**
     * 查询柜机类型列表
     */
    @Operation(summary = "查询柜机类型列表")
    @GetMapping("/list")
    public TableDataInfo list(CabinetType cabinetType)
    {
        //开启分页
        startPage();
        //查询数据
        List<CabinetType> list = cabinetTypeService.selectCabinetTypeList(cabinetType);
        return getDataTable(list);
    }

    /*
    * 添加
    * */
    @Operation(summary = "添加柜机类型")
    @PostMapping
    public AjaxResult add(@RequestBody CabinetType cabinetType){
        boolean isSuccess = cabinetTypeService.save(cabinetType);
        return toAjax(isSuccess);
    }


    /*
    * 根据id查询详细信息
    * */
    @Operation(summary = "根据id查询详细信息")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(cabinetTypeService.getById(id));
    }

    /*
    * 修改
    * */
    @Operation(summary = "修改柜机类型")
    @PutMapping
    public AjaxResult update(@RequestBody CabinetType cabinetType){
        boolean isSuccess = cabinetTypeService.updateById(cabinetType);
        return toAjax(isSuccess);
    }

    /*
    * 删除
    * */
    @Operation(summary = "删除柜机类型")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        boolean isSuccess = cabinetTypeService.removeBatchByIds(Arrays.asList(ids));
        return toAjax(isSuccess);
    }

    /*
    * 查询全部柜机类型列表
    * */
    @Operation(summary = "查询全部柜机类型列表")
    @GetMapping("/getCabinetTypeList")
    public AjaxResult getCabinetTypeList()
    {
        return success(cabinetTypeService.list());
    }

}
