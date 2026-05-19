package com.shanyangcode.videoactionservice.controller;


import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.videoactionservice.model.vo.CategoryListResponse;
import com.shanyangcode.videoactionservice.model.vo.VideoListResponse;
import com.shanyangcode.videoactionservice.service.CategoryService;
import com.shanyangcode.videoactionservice.service.VideoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/video/category")
@Slf4j
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @Resource
    private VideoService videoService;

    @GetMapping
    public BaseResponse<List<CategoryListResponse>> category() {
        return ResultUtils.success(categoryService.categoryList());
    }

    @GetMapping("/list")
    public BaseResponse<List<VideoListResponse>> categoryList(@RequestParam Integer categoryId) {
        return ResultUtils.success(videoService.getCategoryVideoList(categoryId));
    }

}
