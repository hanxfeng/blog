package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hanxingfeng.blog.Entity.NodeCount;
import org.hanxingfeng.blog.Mapper.BlogMapper;
import org.hanxingfeng.blog.Service.BlogService;
import org.springframework.stereotype.Service;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, NodeCount> implements BlogService {
}
