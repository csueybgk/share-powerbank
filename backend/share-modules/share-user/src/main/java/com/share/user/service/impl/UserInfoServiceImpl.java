package com.share.user.service.impl;

import java.util.List;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.context.SecurityContextHolder;
import com.share.common.core.exception.ServiceException;

    // 微信小程序配置：appId、secret，用于 wx.login 换 openid
import com.share.user.config.WxMaConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.share.user.mapper.UserInfoMapper;
import com.share.user.domain.UserInfo;
import com.share.user.service.IUserInfoService;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service

    // 用户信息实体：昵称、头像、手机号、openid、免押状态
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService
{
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private WxMaService wxMaService;
    
    @Override
    public List<UserInfo> selectUserInfoList(UserInfo userInfo)
    {
        return userInfoMapper.selectUserInfoList(userInfo);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public UserInfo wxLogin(String code) {
        log.info("【小程序授权】收到登录请求，code={}", code);
        
        String openId = null;
        try {
            //获取openId
            log.info("【小程序授权】调用微信接口，code={}", code);
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            if (sessionInfo == null) {
                log.error("【小程序授权】微信返回结果为空");
                throw new ServiceException("微信登录失败：获取会话信息为空");
            }
            openId = sessionInfo.getOpenid();
            log.info("【小程序授权】成功获取openId={}", openId);
        } catch (Exception e) {
            log.error("【小程序授权】微信登录异常，code={}, error={}", code, e.getMessage(), e);
            throw new ServiceException("微信登录失败：" + e.getMessage());
        }

        UserInfo userInfo = this.getOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getWxOpenId, openId));
        if (null == userInfo) {
            log.info("【小程序授权】新用户，创建账号，openId={}", openId);
            userInfo = new UserInfo();
            userInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            userInfo.setWxOpenId(openId);
            userInfo.setStatus("1");
            boolean saveResult = this.save(userInfo);
            log.info("【小程序授权】新用户保存结果={}, userId={}", saveResult, userInfo.getId());
        } else {
            log.info("【小程序授权】老用户登录，userId={}", userInfo.getId());
        }
        return userInfo;
    }

    @Override

    // 免押金状态校验
    public Boolean isFreeDeposit() {
        //微信支付分
        //https://pay.weixin.qq.com/wiki/doc/apiv3/payscore.php?chapter=18_1&index=2
        // 默认免押金，模拟实现
        UserInfo userInfo = this.getById(SecurityContextHolder.getUserId());
        userInfo.setDepositStatus("1");
        this.updateById(userInfo);
        return true;
    }

}
