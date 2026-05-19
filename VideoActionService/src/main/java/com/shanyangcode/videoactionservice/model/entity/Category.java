package com.shanyangcode.videoactionservice.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 分区表
 * @TableName category
 */
@TableName(value ="category")
@Data
public class Category  implements Serializable {
    /**
     * 分区id
     */
    @TableId(type = IdType.AUTO)
    private Integer categoryId;

    /**
     * 分区名
     */
    private String categoryName;

    /**
     * 
     */
    private Date createTime;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}