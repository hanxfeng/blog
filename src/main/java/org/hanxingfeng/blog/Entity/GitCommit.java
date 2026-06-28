package org.hanxingfeng.blog.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.persistence.*;
import lombok.Data;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "git_commit")
@Data
public class GitCommit {

    @Id
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(name = "repo_name", length = 50)
    private String repoName;

    @Column(name = "commit_sha", length = 1000)
    private String commitSha;

    @Column(name = "commit_message", length = 200)
    private String commitMessage;

    @Column(name = "addtions")
    private Integer addtions;

    private Integer deletions;

    @Column(name = "total_changes")
    private Integer totalChanges;

    @Column(name = "commit_time")
    private LocalDate commitTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
