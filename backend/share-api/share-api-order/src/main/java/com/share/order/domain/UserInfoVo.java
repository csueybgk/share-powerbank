package com.share.order.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 会员信息VO对象 user_info
 *
 * @date 2026-06-06
 */
@Data
@Schema(description = "会员信息")

    // 用户信息实体：昵称、头像、手机号、openid、免押状态
public class UserInfoVo
{

    @Schema(description = "会员ID")
    private Long id;

    /** 微信openId */
    @Schema(description = "微信openId")
    private String wxOpenId;

    /** 会员昵称 */
    @Schema(description = "会员昵称")
    private String nickname;

    /** 性别（1女 2男） */
    @Schema(description = "性别")
    private String gender;

    /** 头像 */
    @Schema(description = "头像")
    private String avatarUrl;

    /** 电话 */
    @Schema(description = "电话")
    private String phone;

    /** 最后一次登录ip */
    @Schema(description = "最后一次登录ip")
    private String lastLoginIp;

    /** 最后一次登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后一次登录时间")
    private Date lastLoginTime;

    /** 押金状态（0：未验证 1：免押金 2：已交押金） */
    @Schema(description = "押金状态")
    private String depositStatus;

    /** 状态（1有效 2禁用） */
    @Schema(description = "状态")
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

}
