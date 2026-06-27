package org.hanxingfeng.blog.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "commit_count")
@Data
public class CommitCount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int additions;

    private int deletions;

    private int totalChanges;

    private LocalDate commitTime;
}
