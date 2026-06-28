package org.hanxingfeng.blog.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Writings {

    // id
    @TableId(type = IdType.AUTO)
    private Long id;

    // 文章标题
    private String title;

    // 文章内容
    private String content;

    // 文章摘要（前二十个字符）
    private String preview;

    // 时间
    private LocalDate date;

    // 点击量
    private int viewCount;
}
