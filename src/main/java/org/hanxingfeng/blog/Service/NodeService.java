package org.hanxingfeng.blog.Service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.hanxingfeng.blog.Entity.Node;
import org.hanxingfeng.blog.dto.NodeDto;

import java.util.List;

public interface NodeService extends IService<Node> {

    /**
     * 用于查询当前笔记标题
     * @return 所有标题
     */
    List<NodeDto> selectTitles();

    /**
     * 查看具体文章
     * @param id 文章的 id
     * @return 文章
     */
    Node selectNode(int id);

    /**
     * 笔记单文件上传，标题由原始文件名和路径拼接而成
     * @param originalName 原始文件名
     * @param path 手动指定的路径
     * @param body 笔记内容
     * @return 是否为更新，true 表示更新，false 表示新增
     */
    boolean uploadNode(String originalName, String path, String body);

    /**
     * 保存或更新笔记，标题已确定
     * @param title 笔记标题
     * @param body 笔记内容
     * @return 是否为更新，true 表示更新，false 表示新增
     */
    boolean saveOrUpdateNode(String title, String body);
}
