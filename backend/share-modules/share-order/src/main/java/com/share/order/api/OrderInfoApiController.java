package com.share.order.api;

import com.github.pagehelper.PageHelper;
import com.share.common.core.context.SecurityContextHolder;
import com.share.common.core.domain.R;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.order.domain.OrderInfo;
import com.share.order.domain.OrderSqlVo;
import com.share.order.service.IOrderInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "订单接口管理")
@RestController
@RequestMapping("/orderInfo")

    // 订单实体类：记录用户借还充电宝的完整信息，对应 order_info 表
public class OrderInfoApiController extends BaseController
{
    @Autowired
    private IOrderInfoService orderInfoService;

    @Operation(summary = "获取未完成订单")
    @RequiresLogin

    // 查询用户未完成订单（充电中 status=0 或待支付 status=1）
    @GetMapping("getNoFinishOrder")
    public AjaxResult getNoFinishOrder() {
        return success(orderInfoService.getNoFinishOrder(SecurityUtils.getUserId()));
    }

    @Operation(summary = "获取未完成订单")
    @GetMapping("getNoFinishOrder/{userId}")
    public R<OrderInfo> getNoFinishOrder(@PathVariable Long userId) {
        return R.ok(orderInfoService.getNoFinishOrder(userId));
    }

    @Operation(summary = "获取订单详细信息")
    @RequiresLogin
    @GetMapping(value = "/getOrderInfo/{id}")
    public AjaxResult getOrderInfo(@PathVariable("id") Long id)
    {
        return success(orderInfoService.getById(id));
    }

    @Operation(summary = "获取用户订单分页列表")
    @RequiresLogin
    @GetMapping("/userOrderInfoList/{pageNum}/{pageSize}")
    public TableDataInfo list(
            @Parameter(name = "pageNum", description = "当前页码", required = true)
            @PathVariable Integer pageNum,
            @Parameter(name = "pageSize", description = "每页记录数", required = true)
            @PathVariable Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);

    // 查询用户订单列表，充电中的订单实时计算使用时长和费用
        List<OrderInfo> list = orderInfoService.selectUserOrderInfoList(SecurityContextHolder.getUserId());
        return getDataTable(list);
    }


    @Operation(summary = "根据订单号获取订单信息")

    // 根据订单号精确查询订单
    @GetMapping("getByOrderNo/{orderNo}")
    public R<OrderInfo> getByOrderNo(@PathVariable String orderNo) {
        OrderInfo orderInfo = orderInfoService.getByOrderNo(orderNo);
        return R.ok(orderInfo);
    }

    @Operation(summary = "模拟支付成功（本地开发用，跳过微信支付）")
    @RequiresLogin

    // 本地开发模拟支付成功：跳过微信支付流程，直接标记订单为已支付
    @PostMapping("/simulatePaySuccess/{orderNo}")
    public AjaxResult simulatePaySuccess(@PathVariable String orderNo) {
        orderInfoService.simulatePaySuccess(orderNo);
        return success();
    }


    // 执行 AI 生成的 SQL 查询订单报表数据，动态识别列名适配前端图表
    @PostMapping(value = "/getOrderCount")
    public R getOrderCount(@RequestBody OrderSqlVo orderSqlVo) {
        Map<String, Object> map = orderInfoService.getOrderCount(orderSqlVo.getSql());
        return R.ok(map);
    }

}
