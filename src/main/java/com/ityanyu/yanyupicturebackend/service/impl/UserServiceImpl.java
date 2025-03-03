package com.ityanyu.yanyupicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ityanyu.yanyupicturebackend.domain.User;
import com.ityanyu.yanyupicturebackend.service.UserService;
import com.ityanyu.yanyupicturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author 32845
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-03-03 19:35:03
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




