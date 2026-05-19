package com.shanyangcode.videoactionservice.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.SnowflakeConstant;
import com.shanyangcode.common.exception.BusinessException;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.videoactionservice.constants.MinIOConstant;
import com.shanyangcode.videoactionservice.constants.ThreadPoolExecutorConstant;
import com.shanyangcode.videoactionservice.mapper.FileMapper;
import com.shanyangcode.videoactionservice.model.dto.InitUploadRequest;
import com.shanyangcode.videoactionservice.model.dto.MergeChunkRequest;
import com.shanyangcode.videoactionservice.model.entity.File;
import com.shanyangcode.videoactionservice.service.FileService;
import com.shanyangcode.videoactionservice.utils.MinioUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private MinioUtil minioUtil;

    @Override
    public String checkFileExistence(String fileHash) {
        if (StringUtils.isBlank(fileHash)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 根据文件 hash 查询文件
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(File::getFileHash, fileHash);
        File file = this.getOne(queryWrapper);

        return file != null ? file.getFileUrl() : null;
    }

    @Override
    public List<String> getUploadUrls(InitUploadRequest initUploadRequest) {
        // 1. 参数校验（提前暴露非法输入）
        if (initUploadRequest == null) {
            throw new IllegalArgumentException("初始化上传请求不能为空");
        }
        String fileHash = initUploadRequest.getFileHash();
        int chunkCount = initUploadRequest.getChunkCount();
        if (StringUtils.isBlank(fileHash)) {
            throw new IllegalArgumentException("文件哈希值不能为空");
        }
        if(chunkCount <= 0) {
            throw new IllegalArgumentException("分片数量必须大于0，当前值：" + chunkCount);
        }

        // 2. 使用 CompletableFuture 处理异步任务
        List<CompletableFuture<String>> futures = new ArrayList<>(chunkCount);
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            final int index = chunkIndex;
            // 提交异步任务
            CompletableFuture<String> future = CompletableFuture.supplyAsync(
                    // 第一个参数：异步任务要执行的代码
                    () -> minioUtil.uploadChunkUrl(fileHash, index, MinIOConstant.VIDEO_EXPIRE_TIME, TimeUnit.MINUTES),
                    // 第二个参数：使用自定义线程池执行任务（不使用JDK默认线程池）
                    threadPoolExecutor
            ).exceptionally(e -> {
                // 异常处理：记录具体分片错误，后续统一抛出
                log.error("生成分片[{}]上传URL失败", index, e);
                throw new CompletionException("分片[" + index + "]生成URL失败", e);
            });
            futures.add(future);
        }
        // 3. 等待所有任务完成（带超时控制）
        // 把【所有异步小任务】打包成一个【总任务】
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );
        try {
            // 等待超时：使用线程池配置的超时时间
            allFutures.get(ThreadPoolExecutorConstant.AWAIT_TERMINATION, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("生成分片URL超时（{}秒），已完成{}个分片",
                    ThreadPoolExecutorConstant.AWAIT_TERMINATION,
                    futures.stream().filter(CompletableFuture::isDone).count());
            throw new RuntimeException("生成分片URL超时，请重试", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("生成URL操作被中断", e);
        } catch (ExecutionException e) {
            // 捕获子任务的异常（由exceptionally抛出的CompletionException）
            throw new RuntimeException("生成分片URL失败", e.getCause());
        }

        // 4. 收集结果（按分片顺序排列）
        return futures.stream()
                .map(CompletableFuture::join) // 此时已完成，不会阻塞
                .collect(Collectors.toList());
    }

    @Override
    public Set<Integer> getUploadProgress(String fileHash) {
        return minioUtil.getChunkProgress(fileHash);
    }

    @Override
    public String mergeChunk(MergeChunkRequest mergeChunkRequest) {
        String fileHash = mergeChunkRequest.getFileHash();
        int chunkCount = mergeChunkRequest.getChunkCount();
        String fileType = mergeChunkRequest.getFileType();

        // 合并文件
        String url = minioUtil.mergeChunk(fileHash, chunkCount, fileType);

        // 保存文件信息到MySQL
        File file = new File();
        //雪花算法保障生成的文件id全局唯一
        Snowflake snowflake = IdUtil.getSnowflake(SnowflakeConstant.WORKER_ID, SnowflakeConstant.DATA_CENTER_ID);
        file.setFileId(snowflake.nextId());
        file.setFileHash(fileHash);
        file.setFileUrl(url);
        boolean save = this.save(file);
        ThrowUtils.throwIf(!save, ErrorCode.PERSISTENCE_ERROR);

        return url;
    }
}
