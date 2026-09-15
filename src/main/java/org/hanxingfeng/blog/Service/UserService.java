package org.hanxingfeng.blog.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.hanxingfeng.blog.Entity.User;

public interface UserService extends IService<User> {

    /**
     * 校验用户名和密码
     * @param userName 用户名
     * @param password 明文密码
     * @return 校验通过的用户
     */
    User login(String userName, String password);
}
