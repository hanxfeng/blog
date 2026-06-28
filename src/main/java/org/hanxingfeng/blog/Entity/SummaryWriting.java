package org.hanxingfeng.blog.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SummaryWriting {

    // id
    @TableId(type = IdType.AUTO)
    private Long id;

    // 文章标题
    private String title;

    // 文章摘要（前二十个字符）
    private String preview;

    // 时间
    private LocalDate date;

    // 是否置顶
    private int isTop;

}
