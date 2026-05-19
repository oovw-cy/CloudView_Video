package com.shanyangcode.videoactionservice.model.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;


/**
 * 获取文件上传的 url DTO
 */
@Data
public class InitUploadRequest implements Serializable {

    /**
     * 文件哈希
     */
    @NotBlank(message = "文件哈希不能为空")
    private String fileHash;


    /**
     * 初始化分片数量
     */
    @Min(value = 1, message = "分片数量不能小于1")
    private int chunkCount;
}
