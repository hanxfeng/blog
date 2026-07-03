package org.hanxingfeng.blog.dto;

import lombok.Data;
import org.hanxingfeng.blog.Entity.Node;

import java.util.List;

@Data
public class NodeDto {
    private List<String> path;
    private String title;
    private int id;
}
