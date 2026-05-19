package com.shanyangcode.videoactionservice.service;



import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.videoactionservice.model.dto.SendBulletRequest;
import com.shanyangcode.videoactionservice.model.entity.Bullet;
import com.shanyangcode.videoactionservice.model.vo.OnlineBulletResponse;

import java.util.List;


/**
* @author 717
* @description 针对表【bullet(弹幕表)】的数据库操作Service
* @createDate 2026-05-06 13:34:29
*/
public interface BulletService extends IService<Bullet> {

    void saveBulletToMySQL(SendBulletRequest sendBulletRequest);

    List<OnlineBulletResponse> getBulletList(Long videoId);

    boolean bulletExists(Long bulletId);

}
