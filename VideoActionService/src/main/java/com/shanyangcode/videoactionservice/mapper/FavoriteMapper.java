package com.shanyangcode.videoactionservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.shanyangcode.videoactionservice.model.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 717
* @description 针对表【favorite(收藏表)】的数据库操作Mapper
* @createDate 2026-05-07 16:30:37
* @Entity generator.domain.Favorite
*/
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

}




