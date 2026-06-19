package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hanxingfeng.blog.Entity.User;
import org.hanxingfeng.blog.Mapper.UserMapper;
import org.hanxingfeng.blog.Service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
