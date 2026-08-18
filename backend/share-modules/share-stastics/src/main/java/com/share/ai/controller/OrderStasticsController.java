package com.share.ai.controller;

import com.share.common.core.domain.R;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;

import com.share.order.api.RemoteOrderInfoService;
import com.share.order.domain.OrderSqlVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Tag(name = "AI数据统计")
@RestController

    // AI 订单报表：用户输入自然语言需求 → DeepSeek AI 生成 SQL → 执行查询 → 返回图表数据
public class OrderStasticsController extends BaseController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RemoteOrderInfoService orderInfoService;

    @GetMapping("/orderData")
    public AjaxResult generate(@RequestParam(value = "message", defaultValue = "hello")
                               String message) {
        try {
            // 使用restTemplate调用ai接口获取生成的sql语句
            String url = UriComponentsBuilder
                    .fromHttpUrl("http://localhost:8899/ai/generate")
                    .queryParam("message", message)
                    .build()
                    .encode()
                    .toUriString();
            String sql = restTemplate.getForObject(url, String.class);
            // 远程调用根据语句查询数据
            OrderSqlVo orderSqlVo = new OrderSqlVo();
            orderSqlVo.setSql(sql);

    // 执行 AI 生成的 SQL 查询订单报表数据，动态识别列名适配前端图表
            R<Map<String, Object>> result = orderInfoService.getOrderCount(orderSqlVo);
            Map<String, Object> map = result.getData();
            return success(map);
        } catch (Exception e) {
            return error("AI报表查询失败：" + e.getMessage());
        }
    }

}
