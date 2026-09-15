package org.hanxingfeng.blog.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface WritingsService extends IService<org.hanxingfeng.blog.Entity.Writings> {

    /**
     * 获取指定文章，并将点击量加一
     * @param id 文章的 id
     * @return 文章
     * @throws JsonProcessingException
     */
    org.hanxingfeng.blog.Entity.Writings getWritingsById(Long id) throws JsonProcessingException;
}
