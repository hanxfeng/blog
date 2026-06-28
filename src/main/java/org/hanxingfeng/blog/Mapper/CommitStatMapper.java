package org.hanxingfeng.blog.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hanxingfeng.blog.Entity.GitCommit;

@Mapper
public interface CommitStatMapper extends BaseMapper<GitCommit> {
}
