package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hanxingfeng.blog.Entity.CommitCount;
import org.hanxingfeng.blog.Mapper.CommitCountMapper;
import org.hanxingfeng.blog.Service.CommitCountService;
import org.springframework.stereotype.Service;

@Service
public class CommitCountServiceImpl extends ServiceImpl<CommitCountMapper, CommitCount> implements CommitCountService {
}
