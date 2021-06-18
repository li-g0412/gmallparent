package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserInfo;

/**
 * @author atguigu-mqx
 */
public interface UserService {

    //  登录的数据接口：select * from user_info where name = ? and pwd = ?
    //  UserInfo login (String userName, String pwd);
    UserInfo login (UserInfo userInfo);
}
