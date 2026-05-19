package com.shanyangcode.gateway.filter;

import com.shanyangcode.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class AuthorizeFilter implements GlobalFilter, Ordered {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    List<String> excludePaths = Arrays.asList(
            "/api/user/sendVerificationCode",
            "/api/user/register",
            "/api/user/info",
            "/api/user/loginCode",
            "/api/user/focus/list",
            "/api/user/fans/list",
            "/api/user/loginPassword",
            "/api/video/list",
            "/api/video/detail",
            "/api/video/comment/list",
            "/api/video/submit/list",
            "/api/video/coin/list",
            "/api/video/like/list",
            "/api/video/favorite/list",
            "/api/category",
            "/api/category/list",
            "/api/search/video",
            "/api/search/user"
    );


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求参数
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String token = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (excludePaths.contains(path)) {
            return chain.filter(exchange);
        }

        Claims claims = JwtUtil.parse(token);
        if (claims == null) {
            log.warn("JWT parse/token validation failed");
            return response(exchange);
        }

        String userId = claims.getSubject();
        String redisToken = stringRedisTemplate.opsForValue().get(userId);
        if (token.equals(redisToken)) {
            log.info("token合法");
            return chain.filter(exchange);
        }

        log.warn("JWT parse/token validation failed");
        return response(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    public Mono<Void> response(ServerWebExchange exchange) {
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap("{\"code\":401,\"message\":\"未授权的访问，请提供有效的Token\"}".getBytes(StandardCharsets.UTF_8));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}