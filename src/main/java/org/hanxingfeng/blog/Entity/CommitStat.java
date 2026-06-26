package org.hanxingfeng.blog.Entity;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
@Table(name = "commit_stat")
@Data
public class CommitStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String commitSha;

    @Column(columnDefinition = "TEXT")
    private String commitMessage;

    private Integer additions;

    private Integer deletions;

    private Integer totalChanges;

    private LocalDateTime commitTime;

    private LocalDateTime createdAt;
}
