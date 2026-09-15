package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hanxingfeng.blog.Entity.User;
import org.hanxingfeng.blog.Mapper.UserMapper;
import org.hanxingfeng.blog.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 校验用户名和密码
     * @param userName 用户名
     * @param password 明文密码
     * @return 校验通过的用户
     */
    @Override
    public User login(String userName, String password) {
        // 查询数据库中的账号密码
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUserName, userName);
        User sqlUser = userMapper.selectOne(qw);

        if (sqlUser == null || !passwordEncoder.matches(password, sqlUser.getPassword())) {
            throw new IllegalArgumentException("用户不存在或密码错误！");
        }

        return sqlUser;
    }
}
