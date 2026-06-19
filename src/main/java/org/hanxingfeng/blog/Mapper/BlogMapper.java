package org.hanxingfeng.blog.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.hanxingfeng.blog.Entity.NodeCount;

@Mapper
public interface BlogMapper extends BaseMapper<NodeCount> {
}
