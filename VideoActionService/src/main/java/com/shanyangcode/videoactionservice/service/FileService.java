package com.shanyangcode.videoactionservice.service;



import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.videoactionservice.model.dto.InitUploadRequest;
import com.shanyangcode.videoactionservice.model.dto.MergeChunkRequest;
import com.shanyangcode.videoactionservice.model.entity.File;

import java.util.List;
import java.util.Set;

public interface FileService  extends IService<File> {
    String checkFileExistence(String fileHash);
    List<String> getUploadUrls (InitUploadRequest initUploadRequest);
    Set<Integer> getUploadProgress(String fileHash);
    String mergeChunk(MergeChunkRequest mergeChunkRequest);
}
