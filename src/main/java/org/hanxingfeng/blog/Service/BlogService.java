package org.hanxingfeng.blog.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hanxingfeng.blog.Entity.NodeCount;

import java.util.List;

public interface BlogService extends IService<NodeCount> {
    NodeCount selectYesterday() throws Exception;
    List<NodeCount> select() throws JsonProcessingException;
}
