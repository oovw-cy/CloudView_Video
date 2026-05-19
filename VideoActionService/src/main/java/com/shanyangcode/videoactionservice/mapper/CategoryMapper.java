package com.shanyangcode.videoactionservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shanyangcode.videoactionservice.model.entity.Category;
import org.apache.ibatis.annotations.Mapper;


/**
* @author 717
* @description 针对表【category(分区表)】的数据库操作Mapper
* @createDate 2026-05-07 09:28:33
* @Entity generator.domain.CategoryController
*/
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

}




