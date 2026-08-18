package com.share.user.api;

import com.share.common.core.context.SecurityContextHolder;
import com.share.common.core.domain.R;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.InnerAuth;
import com.share.common.security.annotation.RequiresLogin;
import com.share.user.domain.UpdateUserLogin;
import com.share.user.domain.UserInfo;
import com.share.user.domain.UserVo;
import com.share.user.service.IUserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/userInfo")

    // 用户信息实体：昵称、头像、手机号、openid、免押状态
public class UserInfoApiController extends BaseController {

    @Autowired
    private IUserInfoService userInfoService;


    @Operation(summary = "小程序授权登录")
    @GetMapping("/wxLogin/{code}")
    public R<UserInfo> wxLogin(@PathVariable String code) {
        return R.ok(userInfoService.wxLogin(code));
    }


    @Operation(summary = "获取当前登录用户信息")
    @RequiresLogin

    // 获取当前登录用户的详细信息
    @GetMapping("/getLoginUserInfo")
    public AjaxResult getLoginUserInfo(HttpServletRequest request) {
        Long userId = SecurityContextHolder.getUserId();
        UserInfo userInfo = userInfoService.getById(userId);
        UserVo userInfoVo = new UserVo();
        BeanUtils.copyProperties(userInfo, userInfoVo);
        return success(userInfoVo);
    }

    @Operation(summary = "是否免押金")
    @RequiresLogin

    // 免押金状态校验
    @GetMapping("/isFreeDeposit")
    public AjaxResult isFreeDeposit() {
        return success(userInfoService.isFreeDeposit());
    }

    @Operation(summary = "获取用户详细信息")
    @GetMapping(value = "/getUserInfo/{id}")
    public R<UserInfo> getInfo(@PathVariable("id") Long id) {
        UserInfo userInfo = userInfoService.getById(id);
        return R.ok(userInfo);
    }

}
