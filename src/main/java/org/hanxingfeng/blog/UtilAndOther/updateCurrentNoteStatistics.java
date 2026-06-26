package org.hanxingfeng.blog.UtilAndOther;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class updateCurrentNoteStatistics {

    @Autowired
    private UpdateNowData updateNowData;

    @Scheduled(cron = "0 6 17 * * ?")
    public void as () throws Exception {
        updateNowData.updateNoteData();
    }
}
