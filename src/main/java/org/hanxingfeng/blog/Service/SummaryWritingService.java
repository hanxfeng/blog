package org.hanxingfeng.blog.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hanxingfeng.blog.Entity.SummaryWriting;
import org.hanxingfeng.blog.Entity.Writings;

public interface SummaryWritingService extends IService<SummaryWriting> {

    /**
     * 保存文章和对应的摘要
     * @param writings 文章
     * @param summaryWriting 摘要
     */
    void saveWriting(Writings writings, SummaryWriting summaryWriting);

    /**
     * 删除指定文章及其摘要
     * @param id 文章的 id
     */
    void deleteWriting(Long id);

    /**
     * 上传 markdown 文章
     * @param fileName 原始文件名（含 .md 后缀）
     * @param content 文章内容
     * @throws JsonProcessingException
     */
    void uploadWriting(String fileName, String content) throws JsonProcessingException;

    /**
     * 获取所有文章摘要
     * @param current 当前页码
     * @return 分页后的文章摘要
     * @throws JsonProcessingException
     */
    Page<SummaryWriting> getBriefWritings(int current) throws JsonProcessingException;

    /**
     * 用于模糊查询，即搜索
     * @param keyLike 标题关键字
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return 分页后的文章摘要
     */
    Page<SummaryWriting> selectWriting(String keyLike, Integer pageNum, Integer pageSize);

    /**
     * 对文章内容进行修改
     * @param writings 文章
     * @param upOrIn 用于标识是插入新文章还是更新旧文章，0 为插入 1 为更新
     * @throws JsonProcessingException
     */
    void updateWriting(Writings writings, int upOrIn) throws JsonProcessingException;

    /**
     * 用于置顶或取消置顶文章
     * @param id 要置顶的文章的 id
     * @param isTop 1 为置顶，0 为取消置顶
     */
    void topWriting(int id, int isTop);
}
