package com.shanyangcode.videoactionservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.shanyangcode.videoactionservice.mapper.CategoryMapper;
import com.shanyangcode.videoactionservice.model.entity.Category;
import com.shanyangcode.videoactionservice.model.vo.CategoryListResponse;
import com.shanyangcode.videoactionservice.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author 717
* @description 针对表【category(分区表)】的数据库操作Service实现
* @createDate 2026-05-07 09:28:33
*/
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<CategoryListResponse> categoryList() {
        // 1. 查询数据库中【所有】视频分类数据（返回数据库实体对象）
        List<Category> categoryList = this.list();

        // 2. 使用 Java8 Stream 流，将 数据库实体 → 前端响应VO，转换并返回新集合
        return categoryList.stream()
                // map：遍历每个分类对象，做类型转换
                .map(category -> {
                    // 创建前端需要的响应对象
                    CategoryListResponse categoryListResponse = new CategoryListResponse();
                    // 赋值：分类ID（前端需要用它筛选视频）
                    categoryListResponse.setCategoryId(category.getCategoryId());
                    // 赋值：分类名称（前端展示的文字：游戏/美食）
                    categoryListResponse.setCategoryName(category.getCategoryName());
                    // 返回转换后的对象
                    return categoryListResponse;
                })
                // 将转换后的所有对象，收集成 List 集合返回
                .collect(Collectors.toList());
    }

}




