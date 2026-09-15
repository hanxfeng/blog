package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.Node;
import org.hanxingfeng.blog.Mapper.NodeMapper;
import org.hanxingfeng.blog.Service.NodeService;
import org.hanxingfeng.blog.dto.NodeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class NodeServiceImpl extends ServiceImpl<NodeMapper, Node> implements NodeService {

    @Autowired
    private NodeMapper nodeMapper;
    // TODO:添加缓存，上线node，不急
    /**
     * 用于查询当前笔记标题
     * @return 所有标题
     */
    @Override
    public List<NodeDto> selectTitles() {
        LambdaQueryWrapper<Node> qw = new LambdaQueryWrapper<>();
        qw.isNotNull(Node::getTitle);
        List<Node> nodes = nodeMapper.selectList(qw);

        List<NodeDto> nodeDtos = new ArrayList<>();
        for (Node node : nodes) {
            String[] parts = node.getTitle().split("/");

            // 文件名（最后一段）
            String fileName = parts[parts.length - 1];
            // 去掉扩展名（最后一个点之前）
            int dotIdx = fileName.lastIndexOf('.');
            String title = dotIdx == -1 ? fileName : fileName.substring(0, dotIdx);

            // 路径（除文件名外的前缀部分）
            List<String> path = parts.length > 1
                    ? Arrays.asList(Arrays.copyOfRange(parts, 0, parts.length - 1))
                    : Collections.emptyList();

            NodeDto nodeDto = new NodeDto();
            nodeDto.setId(node.getId());
            nodeDto.setTitle(title);
            nodeDto.setPath(path);
            nodeDtos.add(nodeDto);
        }
        return nodeDtos;
    }

    /**
     * 查看具体文章
     * @param id 文章的 id
     * @return 文章
     */
    @Override
    public Node selectNode(int id) {
        log.info("获取id为：{} 的文章",id);
        Node node = nodeMapper.selectById(id);
        return node;
    }

    /**
     * 笔记单文件上传，标题由原始文件名和路径拼接而成
     * @param originalName 原始文件名
     * @param path 手动指定的路径
     * @param body 笔记内容
     * @return 是否为更新，true 表示更新，false 表示新增
     */
    @Override
    public boolean uploadNode(String originalName, String path, String body) {
        String title = buildNodeTitle(originalName, path);
        boolean isUpdate = saveOrUpdateNode(title, body);
        log.info("笔记上传成功: {}", title);
        return isUpdate;
    }

    /**
     * 保存或更新笔记，标题已确定
     * @param title 笔记标题
     * @param body 笔记内容
     * @return 是否为更新，true 表示更新，false 表示新增
     */
    @Override
    public boolean saveOrUpdateNode(String title, String body) {
        LambdaQueryWrapper<Node> qw = new LambdaQueryWrapper<>();
        qw.eq(Node::getTitle, title);
        Node existing = nodeMapper.selectOne(qw);

        if (existing != null) {
            existing.setBody(body);
            nodeMapper.updateById(existing);
            return true;
        }
        Node node = new Node();
        node.setTitle(title);
        node.setBody(body);
        nodeMapper.insert(node);
        return false;
    }

    /**
     * 拼接笔记标题
     * @param originalName 原始文件名
     * @param path 手动指定的路径
     * @return 拼接后的标题
     */
    private String buildNodeTitle(String originalName, String path) {
        if (path == null || path.trim().isEmpty()) {
            return originalName;
        }
        path = path.trim().replace('\\', '/');
        if (path.startsWith("/")) path = path.substring(1);
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path.isEmpty() ? originalName : path + "/" + originalName;
    }
}
