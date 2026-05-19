package com.shanyangcode.videoactionservice.utils;

import cn.hutool.core.util.StrUtil;

import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.exception.ThrowUtils;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MinioUtil {
    @Resource
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.url}")
    private String url;

    /**
     * @MethodName ensureBucketExists
     * @Description 确保桶存在
     * @param: bucketName
     * @return: boolean
     */
    public boolean ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            return true; // 无论是否新建，只要最终桶存在即返回true
        } catch (Exception e) {
            throw new RuntimeException("Bucket操作失败", e);
        }
    }

    /**
     * @MethodName uploadChunkUrl
     * @Description 获取分片上传临时链接
     * @param: fileHash
     * @param: countIndex
     * @param: expires
     * @param: timeUnit
     * @param: fileType
     * @return: String
     */
    public String uploadChunkUrl(String fileHash, int countIndex, Integer expires, TimeUnit timeUnit) {
        // 1. 拼接 MinIO 里的文件路径 = 文件哈希值 / 分片序号
        // 例：文件hash=abc123，第0号分片 → 路径：abc123/0
        String objectName = String.format("%s/%s", fileHash, countIndex);

        // 2. 确保 MinIO 的存储桶(bucket)存在，不存在就自动创建
        ensureBucketExists();
        try {
            // 3. 核心：调用 MinIO 客户端，生成【预签名授权URL】
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)       // 请求方式：PUT 代表上传文件
                            .bucket(bucketName)      // 指定存储桶
                            .object(objectName)      // 指定文件分片的存储路径
                            .expiry(expires, timeUnit) // 设置链接过期时间
                            .build()
            );
        } catch (Exception e) {
            // 4. 生成失败，抛出异常，提示前端/调用方
            throw new RuntimeException("生成临时链接失败", e);
        }
    }

    /**
     * @MethodName getChunkProgress
     * @Description 获取分片上传进度
     * @param: fileHash
     * @return: Set<Integer>
     */
    public Set<Integer> getChunkProgress(String fileHash) {
        // ====================== 1. 前置保障 ======================
        // 确保 MinIO 存储桶存在
        ensureBucketExists();
        // ====================== 2. 初始化结果容器 ======================
        // 创建 HashSet 存储【已上传的分片序号】
        // 用 Set 原因：自动去重、查询快、保证分片序号唯一
        Set<Integer> chunks = new HashSet<>();
        // ====================== 3. 拼接分片存储路径前缀 ======================
        // 核心：分片存储规则是 「文件hash/分片序号」
        // 例：fileHash=abc123 → 前缀 = abc123/
        // 作用：精准定位到这个文件的**所有分片**，不查其他文件
        String prefix = fileHash + "/";
        try {
            // ====================== 4. 调用 MinIO API 查询文件 ======================
            // listObjects：MinIO 列出存储桶内对象的核心方法
            // Iterable<Result<Item>>：返回分页迭代器（MinIO 自动分页处理大量文件）
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)      // 指定要查询的存储桶
                            .prefix(prefix)          // 只查询以 xxx/ 开头的对象（精准查当前文件的分片）
                            .recursive(false)        // 不递归查询子目录（你的分片没有子目录，关闭提升性能）
                            .build()
            );

            // ====================== 5. 遍历查询结果，解析分片序号 ======================
            // 迭代器遍历 MinIO 返回的所有分片对象
            for (Result<Item> result : results) {
                // result.get()：获取实际的文件/目录对象信息（处理 MinIO 包装类）
                Item item = result.get();

                // 排除「目录」，只处理「真实的分片文件」
                // 你的分片是文件，不是目录，所以过滤掉目录项
                if (!item.isDir()) {
                    // 获取对象的完整路径：例 abc123/0
                    String objectName = item.objectName();

                    // 截取最后一个 / 后面的字符串 → 分片序号（例：0、1、2）
                    String chunkIndex = objectName.substring(objectName.lastIndexOf("/") + 1);

                    // 把分片序号转为 Integer，存入集合
                    chunks.add(Integer.valueOf(chunkIndex));
                }
            }
        } catch (Exception e) {
            // ====================== 6. 统一异常处理 ======================
            // 捕获所有异常（网络、MinIO 错误、格式转换错误），抛出友好提示
            throw new RuntimeException("获取分片列表失败: " + e.getMessage(), e);
        }
        return chunks;
    }

    /**
     * @MethodName mergeChunk
     * @Description 合并分片
     * @param: fileHash   文件唯一哈希值（定位所有分片）
     * @param: chunkCount 前端声明的**总分片数量**
     * @param: fileType   文件后缀（如 mp4/jpg/png）
     * @return: String    合并后完整文件的访问地址
     */
    public String mergeChunk(String fileHash, int chunkCount, String fileType) {
        // ====================== 1. 基础准备 ======================
        // 确保MinIO桶存在
        ensureBucketExists();

        // 最终合并后的文件在MinIO中的基础名称 = 文件哈希值
        String ObjectName = fileHash;
        // 总分片数量
        int count = chunkCount;

        // ====================== 2. 校验分片完整性（核心！） ======================
        // 调用之前写的 getChunkProgress 方法，获取【已上传的分片数量】
        int chunkLists = getChunkProgress(fileHash).size();
        // 校验：前端声明的总分片数 = 实际已上传的分片数
        // 不相等 → 抛出合并文件异常（分片缺失，无法合并）
        ThrowUtils.throwIf(count != chunkLists, ErrorCode.MERGE_FILE_ERROR);

        // ====================== 3. 组装分片合并参数 ======================
        // ComposeSource：MinIO提供的对象，代表**一个待合并的分片源**
        List<ComposeSource> composeSources = new ArrayList<>();
        // 存储所有临时分片的路径，用于后续删除
        List<String> objectNames = new ArrayList<>();

        // 遍历所有分片（0、1、2...count-1）
        for (int i = 0; i < count; i++) {
            // 拼接分片路径：规则 = 文件hash/分片序号（和上传时完全一致）
            String objectName = String.format("%s/%s", ObjectName, i);
            // 存入临时分片路径集合
            objectNames.add(objectName);
            // 构建MinIO合并需要的分片源：指定桶 + 分片路径
            composeSources.add(
                    ComposeSource.builder()
                            .bucket(bucketName)      // 分片所在的桶
                            .object(objectName)      // 分片的完整路径
                            .build()
            );
        }

        // 二次校验：组装的分片数量 = 总分片数（防止中途分片丢失）
        ThrowUtils.throwIf(count != composeSources.size(), ErrorCode.CHUNK_FILE_LACK);

        // ====================== 4. 构建最终文件 ======================
        // 最终文件名 = 文件hash.后缀（例：abc123.mp4）
        String finalObjectName = fileHash + "." + fileType;

        // 设置HTTP响应头（浏览器访问文件时用）
        Map<String, String> headers = new HashMap<>();
        // 根据文件后缀，设置正确的Content-Type（告诉浏览器文件类型：视频/图片/文档）
        headers.put("Content-Type", new ContentTypeUtil().getType(fileType));
        // inline：浏览器直接预览（而非下载）
        headers.put("Content-Disposition", "inline");

        // 构建MinIO合并文件的参数对象
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket(bucketName)              // 合并后文件存放的桶
                .object(finalObjectName)         // 合并后的文件名
                .sources(composeSources)         // 所有待合并的分片
                .headers(headers)                // 文件响应头
                .build();

        // ====================== 5. 执行合并 + 清理临时文件 ======================
        try {
            // 🔥 MinIO核心API：服务端直接合并所有分片（高性能，无需下载/上传）
            minioClient.composeObject(composeObjectArgs);

            // 合并成功后，**删除所有临时分片**（释放存储空间）
            for (String objectName : objectNames) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
            }
        } catch (Exception e) {
            // 合并/删除失败，抛出异常
            throw new RuntimeException("合并文件分片失败", e);
        }

        // ====================== 6. 返回完整文件的访问地址 ======================
        return downloadUrl(finalObjectName);
    }

    /**
     * @MethodName downloadUrl
     * @Description 获取下载/访问链接
     * @param: fileName 合并后的完整文件名（abc123.mp4）
     * @return: String  文件的公开访问地址
     */
    public String downloadUrl(String fileName) {
        // 拼接规则：MinIO服务地址 + / + 桶名 + / + 文件名
        // 例：http://localhost:9000/tianmu-upload/abc123.mp4
        return url + StrUtil.SLASH + bucketName + StrUtil.SLASH + fileName;
    }

    /**
     * @MethodName updateCover
     * @Description 上传封面
     * @param: file
     * @return: String
     */
    public String updateCover(MultipartFile file) throws Exception {
        ensureBucketExists();
        String fileSuffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);//获取文件后缀名（如：jpg、png、jpeg）， 截取文件名最后一个.后面的字符串
        String id = UUID.randomUUID().toString();// 生成唯一ID（UUID），防止文件重名覆盖
        String fileName = id + "." + fileSuffix;//拼接新文件名：唯一ID + 后缀名（例：a1b2c3.jpg）
        InputStream inputStream = file.getInputStream();//  获取文件的输入流（读取文件二进制数据）
        String contentType = new ContentTypeUtil().getType(fileSuffix);// 根据文件后缀，获取对应的ContentType（浏览器识别文件类型）
        try {
            // 7. 核心：调用MinIO客户端，上传文件到服务器
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)       // 指定存储桶（相当于文件夹）
                            .object(fileName)         // 上传后的文件名
                            .stream(                  // 文件流、文件大小、分片大小(-1=自动)
                                    inputStream,
                                    file.getSize(),
                                    -1
                            )
                            .contentType(contentType) // 设置文件类型
                            .build()
            );
        } catch (Exception e) {
            // 8. 上传失败，抛出异常（全局异常处理器捕获）
            throw new RuntimeException("文件上传失败", e);
        }
        return downloadUrl(fileName);// 生成并返回封面的访问URL
    }



}
