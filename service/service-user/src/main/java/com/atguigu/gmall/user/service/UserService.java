package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.user.UserInfo;

import java.util.List;

public interface UserService {

    //  登录的数据接口：select * from user_info where name = ? and pwd = ?
    //  UserInfo login (String userName, String pwd);
    UserInfo login (UserInfo userInfo);

    /**
     * 根据用户Id 查询用户的收货地址列表！
     * @param userId
     * @return
     */
    List<UserAddress> findUserAddressListByUserId(String userId);

}
