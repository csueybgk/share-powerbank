package com.share.user.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.share.user.mapper.UserLoginLogMapper;
import com.share.user.domain.UserLoginLog;
import com.share.user.service.IUserLoginLogService;

/**
 * 用户登录记录Service业务层处理
 *
 * @date 2026-06-03
 */
@Service
public class UserLoginLogServiceImpl extends ServiceImpl<UserLoginLogMapper, UserLoginLog> implements IUserLoginLogService
{
    @Autowired
    private UserLoginLogMapper userLoginLogMapper;

    /**
     * 查询用户登录记录列表
     *
     * @param userLoginLog 用户登录记录
     * @return 用户登录记录
     */
    @Override
    public List<UserLoginLog> selectUserLoginLogList(UserLoginLog userLoginLog)
    {
        return userLoginLogMapper.selectUserLoginLogList(userLoginLog);
    }

}
