package org.hanxingfeng.blog.Entity;

import lombok.Data;

@Data
public class User {
    private Long userId;
    private String userName;
    private String password;
    private int role;
    private String ip;
}
