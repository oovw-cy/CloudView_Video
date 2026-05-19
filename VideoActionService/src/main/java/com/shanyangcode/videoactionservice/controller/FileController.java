package com.shanyangcode.videoactionservice.controller;


import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.videoactionservice.model.dto.InitUploadRequest;
import com.shanyangcode.videoactionservice.model.dto.MergeChunkRequest;
import com.shanyangcode.videoactionservice.service.FileService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 视频上传
 */
@RestController
@RequestMapping("/video/file")
@Slf4j
public class FileController {
    @Resource
    private FileService fileService;

    @GetMapping("/check")
    public BaseResponse<String> checkFileExistence(@RequestParam String fileHash) {
        String fileUrl = fileService.checkFileExistence(fileHash);
        if (fileUrl == null) {
            return ResultUtils.error(ErrorCode.VIDEO_NOT_FOUND_ERROR);
        }
        return ResultUtils.success(fileUrl);
    }
    //获取分片的urls
    @PostMapping("get/upload/urls")
    public BaseResponse<List<String>> getUploadUrls(@Valid @RequestBody InitUploadRequest initUploadRequest) {
        return ResultUtils.success(fileService.getUploadUrls(initUploadRequest));
    }
    //查询分片进度
    @GetMapping("/get/upload/progress")
    public BaseResponse<Set<Integer>> getUploadProgress(@Valid @RequestParam String fileHash) {
        return ResultUtils.success(fileService.getUploadProgress(fileHash));
    }
    //合并分片为一个url
    @PostMapping("/merge/chunk")
    public BaseResponse<String> mergeChunk(@Valid @RequestBody MergeChunkRequest mergeChunkRequest) {
        return ResultUtils.success(fileService.mergeChunk(mergeChunkRequest));
    }
}
