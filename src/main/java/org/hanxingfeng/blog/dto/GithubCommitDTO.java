package org.hanxingfeng.blog.dto;

import lombok.Data;

@Data
public class GithubCommitDTO {

    private String sha;

    private CommitInfo commit;

    @Data
    public static class CommitInfo {
        private String message;
        private Author author;
    }

    @Data
    public static class Author {
        private String date;
    }
}
