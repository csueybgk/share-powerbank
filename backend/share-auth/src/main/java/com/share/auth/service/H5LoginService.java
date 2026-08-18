package com.share.auth.service;


import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;


import com.share.system.api.model.LoginUser;
import com.share.user.api.RemoteUserInfoService;
import com.share.user.domain.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class H5LoginService
{
    private static final Logger log = LoggerFactory.getLogger(H5LoginService.class);

    @Autowired
    private RemoteUserInfoService remoteUserInfoService;

    @Autowired
    private SysRecordLogService recordLogService;

    /**
     * 登录
     */
    public LoginUser login(String code)
    {
        log.info("【H5登录】开始登录，code={}", code);

        // 查询用户信息
        R<UserInfo> userResult = null;
        try {
            userResult = remoteUserInfoService.wxLogin(code);
            log.info("【H5登录】远程调用返回结果 - code: {}, msg: {}, data: {}", 
                    userResult != null ? userResult.getCode() : "null",
                    userResult != null ? userResult.getMsg() : "null",
                    userResult != null ? userResult.getData() : "null");
        } catch (Exception e) {
            log.error("【H5登录】远程调用异常", e);
            throw new ServiceException("微信登录服务异常：" + e.getMessage());
        }
        
        if (userResult == null || userResult.getCode() != 200) {
            log.error("【H5登录】微信登录接口调用失败，code={}, result={}", code, userResult);
            String errorMsg = userResult != null ? userResult.getMsg() : "未知错误";
            throw new ServiceException("微信登录失败：" + errorMsg);
        }
        
        UserInfo userInfo = userResult.getData();
        log.info("【H5登录】获取到用户信息，userInfo={}", userInfo);

        if(userInfo == null){
            log.error("【H5登录】用户不存在，code={}", code);
            throw new ServiceException("用户不存在");
        }

        if ("2".equals(userInfo.getStatus()))
        {
            log.warn("【H5登录】账号被禁用，userId={}", userInfo.getId());
            throw new ServiceException("账号被禁用");
        }
        
        //封装登录用户信息
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(userInfo.getId());
        loginUser.setUsername(userInfo.getWxOpenId());
        loginUser.setStatus(userInfo.getStatus()+"");

        log.info("【H5登录】登录成功，userId={}", loginUser.getUserid());
        return loginUser;
    }
}
