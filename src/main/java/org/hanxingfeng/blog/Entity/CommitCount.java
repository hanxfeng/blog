package org.hanxingfeng.blog.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "commit_count")
@Data
public class CommitCount {
    @Id
    @TableId(type = IdType.AUTO)
    private int id;

    private int additions;

    private int deletions;

    private int totalChanges;

    private LocalDate commitTime;
}
