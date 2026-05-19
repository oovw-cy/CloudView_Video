package com.shanyangcode.searchsyncservice.job;

import cn.hutool.core.collection.CollUtil;

import com.shanyangcode.searchsyncservice.esdao.UserEsDao;
import com.shanyangcode.searchsyncservice.model.entity.User;
import com.shanyangcode.searchsyncservice.model.es.UserEs;
import com.shanyangcode.searchsyncservice.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FullSyncUserToEs implements CommandLineRunner {

    @Resource
    private UserService userService;

    @Resource
    private UserEsDao userEsDao;

    @Override
    public void run(String... args) {
        // 全量获取用户（数据量不大的情况下使用）
        List<User> userList = userService.list();
        if (CollUtil.isEmpty(userList)) {
            return;
        }
        // 转为 ES 实体类
        List<UserEs> userEsList = userList.stream().map(user -> {
            UserEs userEs = new UserEs();
            userEs.setId(user.getUserId());
            userEs.setNickname(user.getNickname());
            return userEs;
        }).collect(Collectors.toList());

        System.out.println(userEsList);


        // 分页批量插入到 ES
        final int pageSize = 500;
        int total = userList.size();
        log.info("FullSyncUserToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            // 注意同步的数据下标不能超过总数据量
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            userEsDao.saveAll(userEsList.subList(i, end));
        }
        log.info("FullSyncUserToEs end, total {}", total);
    }
}