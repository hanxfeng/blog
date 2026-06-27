package org.hanxingfeng.blog.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hanxingfeng.blog.Entity.CommitCount;

@Mapper
public interface CommitCountMapper extends BaseMapper<CommitCount> {
}
