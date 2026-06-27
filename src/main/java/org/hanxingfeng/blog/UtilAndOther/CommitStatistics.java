package org.hanxingfeng.blog.UtilAndOther;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CommitStatistics {
    @Autowired
    private getGitHubCommit getGitHubCommit;

    @Scheduled(cron = "0 0 10 * * ?")
    public void scheduleCommitStat() {

        commitStatAsync();
    }

    @Async("commitStatExecutor")
    public void commitStatAsync() {
        getGitHubCommit.CommitStatService();
    }
}
