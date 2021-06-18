package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author atguigu-mqx
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;


    @Override
    public UserInfo login(UserInfo userInfo) {
        //  select * from user_info where login_name = ? and passwd = ?
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("login_name",userInfo.getLoginName());

        //  密码是加密的： 96e79218965eb72c92a549dd5a330112
        String passwd = userInfo.getPasswd();
        String newPassword = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfoQueryWrapper.eq("passwd",newPassword);

        UserInfo info = userInfoMapper.selectOne(userInfoQueryWrapper);
        //  判断
        if (info!=null){
            //  返回数据
            return info;
        }
        return null;
    }
}
