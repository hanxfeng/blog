package org.hanxingfeng.blog.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.hanxingfeng.blog.Entity.SummaryWriting;
import org.hanxingfeng.blog.Entity.Writings;

public interface SummaryWritingService extends IService<SummaryWriting> {
    void saveWriting(Writings writings, SummaryWriting summaryWriting);
    void deleteWriting(Long id);
}
