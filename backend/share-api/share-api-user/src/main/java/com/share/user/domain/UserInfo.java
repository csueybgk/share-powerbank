package com.share.user.domain;

import com.share.common.core.annotation.Excel;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户对象 user_info
 *
 * @date 2026-06-03
 */
@Data
@Schema(description = "用户")

    // 用户信息实体：昵称、头像、手机号、openid、免押状态
public class UserInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 微信openId */
    @Excel(name = "微信openId")
    @Schema(description = "微信openId")
    private String wxOpenId;

    /** 会员昵称 */
    @Excel(name = "会员昵称")
    @Schema(description = "会员昵称")
    private String nickname;

    /** 性别 */
    @Excel(name = "性别")
    @Schema(description = "性别")
    private String gender;

    /** 头像 */
    @Excel(name = "头像")
    @Schema(description = "头像")
    private String avatarUrl;

    /** 电话 */
    @Excel(name = "电话")
    @Schema(description = "电话")
    private String phone;

    /** 1有效，2禁用 */
    @Excel(name = "1有效，2禁用")
    @Schema(description = "1有效，2禁用")
    private String status;

    /** 是否免押金 **/
    @Excel(name = "是否免押金")
    @Schema(description = "是否免押金")
    private String depositStatus;

}
