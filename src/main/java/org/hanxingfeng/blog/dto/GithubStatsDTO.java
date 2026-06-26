package org.hanxingfeng.blog.dto;

import lombok.Data;

@Data
public class GithubStatsDTO {
    private Integer total;
    private Integer additions;
    private Integer deletions;
}
