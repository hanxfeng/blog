package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.SummaryWriting;
import org.hanxingfeng.blog.Entity.Writings;
import org.hanxingfeng.blog.Mapper.SummaryWritingMapper;
import org.hanxingfeng.blog.Mapper.WritingsMapper;
import org.hanxingfeng.blog.Service.SummaryWritingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SummaryWritingServiceImpl extends ServiceImpl<SummaryWritingMapper, SummaryWriting> implements SummaryWritingService {

    @Autowired
    private WritingsMapper writingsMapper;

    @Autowired
    private SummaryWritingMapper summaryWritingMapper;

    @Override
    @Transactional
    public void saveWriting(Writings writings, SummaryWriting summaryWriting) {
        writingsMapper.insert(writings);
        summaryWritingMapper.insert(summaryWriting);
        log.info("文件保存成功！");

    }

    @Transactional
    @Override
    public void deleteWriting(Long id) {
        writingsMapper.deleteById(id);
        summaryWritingMapper.deleteById(id);
    }
}
