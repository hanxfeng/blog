package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hanxingfeng.blog.Entity.Writings;
import org.hanxingfeng.blog.Mapper.WritingsMapper;
import org.hanxingfeng.blog.Service.WritingsService;
import org.springframework.stereotype.Service;

@Service
public class WritingsServiceImpl extends ServiceImpl<WritingsMapper, Writings> implements WritingsService {
}
