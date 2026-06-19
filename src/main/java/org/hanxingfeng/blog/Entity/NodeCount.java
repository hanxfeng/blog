package org.hanxingfeng.blog.Entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDate;

@Data
public class NodeCount {
    // 每日字数统计
    private int dailyNoteWordCount;
    // 每日笔记数量统计
    private int dailyNoteCount;
    // 到当前日期总笔记数量
    private int totalNoteCount;
    // 到当前日期总笔记字数
    private int totalNoteWordCount;
    // 时间
    @TableId()
    private LocalDate todayTime;

}
