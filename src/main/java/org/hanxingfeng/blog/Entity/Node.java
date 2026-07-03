package org.hanxingfeng.blog.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "node")
@Data
public class Node {
    @Id
    @TableId(type = IdType.AUTO)
    private int id;

    private String title;

    private String body;
}
