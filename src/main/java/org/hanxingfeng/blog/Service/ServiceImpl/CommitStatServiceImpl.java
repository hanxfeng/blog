package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hanxingfeng.blog.Entity.GitCommit;
import org.hanxingfeng.blog.Mapper.CommitStatMapper;
import org.hanxingfeng.blog.Service.CommitStatService;
import org.springframework.stereotype.Service;

@Service
public class CommitStatServiceImpl extends ServiceImpl<CommitStatMapper, GitCommit> implements CommitStatService {
}
