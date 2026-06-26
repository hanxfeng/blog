package org.hanxingfeng.blog.UtilAndOther;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import static org.hanxingfeng.blog.Entity.SystemConstants.SECRET;

@Component
public class JWTUtil {
    // 随机生成的 key

    private final Key key =
            Keys.hmacShaKeyFor(
                    SECRET.getBytes(StandardCharsets.UTF_8)
            );

    /**
     * 用于生成 token
     * userId:用户 id
     * userName:用户名称
     * @return 返回生成的 token
     */
    public String generateToken(Long userId, String userName) {
        JwtBuilder jwtBuilder = Jwts.builder();

        return jwtBuilder
                // 添加 Payload
                .claim("userName", userName) // 私有声明
                .setSubject(String.valueOf((userId))) // 注册声明
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 一小时后过期
                // 添加 Signature
                .signWith(key) // 这个方法会自动生成 Header 因此不需要再手动写 Header
                // 将以上三部分用 . 拼接
                .compact();
    }

    /**
     * 用于对 token 进行解析
     * @param token：需要解析的 token
     * @return 解析后的内容
     */
    public Claims parseToken(String token) {
        Jws<Claims> claimsJwt = Jwts.parserBuilder()
                .setSigningKey(key)          // SECRET_KEY 可为 String/byte[]/Key
                .build()                            // 构建 JwtParser
                .parseClaimsJws(token);             // 解析并验证 JWS

        return claimsJwt.getBody();
    }

    /**
     * 用于判断 JWT 是否过期
     * @param token：需要进行判断的 token
     * @return 返回 true 表示 token 已过期
     */
    public boolean isExpired(String token) {
        return parseToken(token)          // 1. 解析 JWT，获取 Claims 对象
                .getExpiration()          // 2. 取出过期时间（即 exp 字段）
                .before(new Date());      // 3. 判断该时间是否在当前时间之前
    }
}
