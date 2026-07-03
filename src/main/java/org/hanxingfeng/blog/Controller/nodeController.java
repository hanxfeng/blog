package org.hanxingfeng.blog.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.hanxingfeng.blog.Entity.Node;
import org.hanxingfeng.blog.Entity.R;
import org.hanxingfeng.blog.Mapper.NodeMapper;
import org.hanxingfeng.blog.dto.NodeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/node")
public class nodeController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private NodeMapper nodeMapper;

    /**
     * 用于查询当前笔记标题
     * @return 所有标题
     */
    @GetMapping("/select")
    public R<List<NodeDto>> select() {
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
                    : Collections.emptyList();  // 无路径时用空列表，而不是 null

            NodeDto nodeDto = new NodeDto();
            nodeDto.setId(node.getId());
            nodeDto.setTitle(title);
            nodeDto.setPath(path);
            nodeDtos.add(nodeDto);
        }
        return R.success(nodeDtos);
    }


    /**
     * 查看具体文章
     * @param id 文章的 id
     * @return 文章
     */
    @GetMapping("/selectNode/{id}")
    public R<Node> selectNode(@PathVariable int id) {
        Node node = nodeMapper.selectById(id);
        return R.success(node);
    }


    @PostMapping("/updateNode")
    public R<String> updateNode() {

    }

}
