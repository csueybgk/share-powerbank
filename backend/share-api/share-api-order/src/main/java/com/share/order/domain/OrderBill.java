package com.share.order.domain;

import java.math.BigDecimal;
import com.share.common.core.web.domain.BaseEntity;
import com.share.common.core.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单账单对象 order_bill
 *
 * @date 2026-06-06
 */
@Data
@Schema(description = "订单账单")
public class OrderBill extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    @Excel(name = "订单ID")
    @Schema(description = "订单ID")
    private Long orderId;

    /** 账单项明细 */
    @Excel(name = "账单项明细")
    @Schema(description = "账单项明细")
    private String billItem;

    /** 账单金额 */
    @Excel(name = "账单金额")
    @Schema(description = "账单金额")
    private BigDecimal billAmount;

}
