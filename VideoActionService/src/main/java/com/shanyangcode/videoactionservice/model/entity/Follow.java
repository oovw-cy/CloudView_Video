package com.shanyangcode.videoactionservice.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 关注表
 * @TableName follow
 */
@TableName(value ="follow")
@Data
public class Follow implements Serializable {
    /**
     * 关注ID
     */
    @TableId
    private Long followId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 被关注用户ID
     */
    private Long creatorId;

    /**
     * 
     */
    private Date createTime;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}