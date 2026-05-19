package com.shanyangcode.videoactionservice.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 评论表
 * @TableName comment
 */
@TableName(value ="comment")
@Data
public class Comment implements Serializable {
    /**
     * 评论ID
     */
    @TableId
    private Long commentId;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 父评论ID, 表示是上一级级评论
     */
    private Long parentCommentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 
     */
    private Date createTime;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}