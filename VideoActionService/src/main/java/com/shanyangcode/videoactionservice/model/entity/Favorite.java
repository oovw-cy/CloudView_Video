package com.shanyangcode.videoactionservice.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 收藏表
 * @TableName favorite
 */
@TableName(value ="favorite")
@Data
public class Favorite implements Serializable {
    /**
     * 收藏ID
     */
    @TableId
    private Long favoriteId;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 
     */
    private Date createTime;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}