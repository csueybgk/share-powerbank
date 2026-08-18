package com.share.order.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 订单报表AI查询VO
 *
 * @date 2026-06-10
 */
@Data
@Schema(description = "订单报表AI查询")
public class OrderSqlVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户原始提问消息")
    private String message;

    @Schema(description = "AI生成的SQL查询语句")
    private String sql;

    @Schema(description = "表结构上下文（可选，用于自定义查询范围）")
    private String tableSchema;

    @Schema(description = "查询创建时间")
    private Date createTime;

    @Schema(description = "用户ID（用于权限控制）")
    private Long userId;

    @Schema(description = "会话ID（用于多轮对话关联）")
    private String sessionId;

}
