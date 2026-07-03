package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hanxingfeng.blog.Entity.Node;
import org.hanxingfeng.blog.Mapper.NodeMapper;
import org.hanxingfeng.blog.Service.NodeService;
import org.springframework.stereotype.Service;

@Service
public class NodeServiceImpl extends ServiceImpl<NodeMapper, Node> implements NodeService {
}
