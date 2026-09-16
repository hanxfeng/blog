package org.hanxingfeng.blog.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hanxingfeng.blog.Entity.CommitCount;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CommitCountService extends IService<CommitCount> {

    /**
     * 获取昨日提交数据
     * @return 昨日的提交数据
     * @throws JsonProcessingException
     */
    CommitCount selectOneCommitCount() throws JsonProcessingException;

    /**
     * 获取所有修改代码行数
     * @return 今年以来所有的提交数据
     * @throws JsonProcessingException
     */
    List<CommitCount> selectAllCommit() throws JsonProcessingException;

    /**
     * 用于获取总代码修改行数
     * @return 总代码修改行数
     */
    Map sumCommit() throws JsonProcessingException;
}
