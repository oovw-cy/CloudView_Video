package com.shanyangcode.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "VideoActionService")
public interface VideoActionServiceClient {


    @PostMapping("/api/video/user/follow/type")
    Integer followType(@RequestParam Long userId, @RequestParam Long creatorId);

}