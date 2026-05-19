package com.shanyangcode.videoactionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.videoactionservice.model.entity.Category;
import com.shanyangcode.videoactionservice.model.vo.CategoryListResponse;


import java.util.List;


/**
* @author 717
* @description 针对表【category(分区表)】的数据库操作Service
* @createDate 2026-05-07 09:28:33
*/
public interface CategoryService extends IService<Category> {
    List<CategoryListResponse> categoryList();
}
