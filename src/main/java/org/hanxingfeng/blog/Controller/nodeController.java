package org.hanxingfeng.blog.Controller;

import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.Node;
import org.hanxingfeng.blog.Entity.R;
import org.hanxingfeng.blog.Service.NodeService;
import org.hanxingfeng.blog.dto.NodeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/node")
public class nodeController {

    @Autowired
    private NodeService nodeService;

    /**
     * 用于查询当前笔记标题
     * @return 所有标题
     */
    @GetMapping("/select")
    public R<List<NodeDto>> select() {
        return R.success(nodeService.selectTitles());
    }

    /**
     * 查看具体文章
     * @param id 文章的 id
     * @return 文章
     */
    @GetMapping("/selectNode/{id}")
    public R<Node> selectNode(@PathVariable int id) {
        return R.success(nodeService.selectNode(id));
    }

}
