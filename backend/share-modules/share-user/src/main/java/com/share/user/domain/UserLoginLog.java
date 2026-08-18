package com.share.user.domain;

import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.share.common.core.annotation.Excel;

/**
 * 用户登录记录对象 user_login_log
 *
 * @date 2026-06-03
 */
@Data
@Schema(description = "用户登录记录")
public class UserLoginLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户id */
    @Excel(name = "用户id")
    @Schema(description = "用户id")
    private Long userId;

    /** 登录IP地址 */
    @Excel(name = "登录IP地址")
    @Schema(description = "登录IP地址")
    private String ipaddr;

    /** 登录状态 */
    @Excel(name = "登录状态")
    @Schema(description = "登录状态")
    private Integer status;

    /** 提示信息 */
    @Excel(name = "提示信息")
    @Schema(description = "提示信息")
    private String msg;

}
