package com.shanyangcode.realtimeservice.websocket;



import com.shanyangcode.realtimeservice.producer.RocketMQProducer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.util.NettyRuntime;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class NettyService {
    // 从 application.yml 读取配置的端口号
    @Value("${netty.port}")
    private int port;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);  // 1. 主线程组：只负责【接收客户端连接请求】，不处理业务

    private final EventLoopGroup workerGroup = new NioEventLoopGroup(NettyRuntime.availableProcessors() * 2);   // 2. 工作线程组：负责【数据读写、业务处理】

    private final RocketMQProducer producer;// 依赖注入：RocketMQ生产者（异步处理弹幕消息，不阻塞Netty）

    private final StringRedisTemplate stringRedisTemplate;// 依赖注入：Redis模板（存储在线用户、弹幕缓存、视频房间状态）

    private Channel serverChannel;// 服务端Channel对象

    @PostConstruct
    public void start() {
        // 新建独立线程启动Netty → **不阻塞SpringBoot主线程**（生产级必备）
        new Thread(() -> {
            try {
                run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Netty server start interrupted", e);
            }
        }, "netty-starter").start();
    }

    public void run() throws InterruptedException {

        ServerBootstrap serverBootstrap = new ServerBootstrap();    // 创建服务端启动引导类：负责组装所有 Netty 配置
        // 1. 核心线程组 + 通道类型配置
        serverBootstrap.group(bossGroup,workerGroup)// 设置主从线程组
                .channel(NioServerSocketChannel.class)// 指定使用 NIO 模式的服务器通道
                // 2. ========== TCP参数优化（高并发弹幕必备）==========
                .option(ChannelOption.SO_BACKLOG, 1024)     // 连接等待队列大小
                .option(ChannelOption.SO_REUSEADDR, true)   // 端口复用，快速重启
                .childOption(ChannelOption.TCP_NODELAY, true)// 禁用Nagle算法，弹幕消息实时发送
                .childOption(ChannelOption.SO_KEEPALIVE, true)// TCP心跳保活
                .childOption(ChannelOption.SO_RCVBUF, 1024 * 1024)// 接收缓冲区1M
                .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024) // 发送缓冲区1M
                //3. 服务端日志：打印DEBUG级别网络请求日志
                .handler(new LoggingHandler(LogLevel.DEBUG))

                // 4. 客户端连接初始化流水线（核心！）
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // 通道流水线：责任链模式，数据依次经过这些处理器
                        ChannelPipeline pipeline = ch.pipeline();
                        // ========== 流水线处理器（顺序绝对不能乱）==========
                        // ① 心跳检测：60秒未读事件（客户端未发消息），触发心跳事件
                        pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                        // ② HTTP编解码：WebSocket基于HTTP握手，必须先解析HTTP
                        pipeline.addLast(new HttpServerCodec());
                        // ③ HTTP消息聚合：合并分段HTTP请求，最大64k
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        // ④ WebSocket协议核心处理器（路径：/ws/bulletScreen → 弹幕服务）
                        pipeline.addLast(new WebSocketServerProtocolHandler(
                                "/ws/bulletScreen",  // 服务路径
                                null,                // 子协议
                                true,                // 允许扩展
                                65536,               // 最大消息长度
                                false,               // 允许客户端掩码
                                true                 // 严格WebSocket规范
                        ));
                        // ⑤ 流量控制：单连接限速 1M/s（防止恶意刷屏/超大消息）
                        pipeline.addLast(new ChannelTrafficShapingHandler(1024 * 1024, 1024 * 1024));
                        // ⑥ 自定义业务处理器：处理弹幕消息（注入MQ+Redis）
                        pipeline.addLast(new WebSocketHandler(producer, stringRedisTemplate));
                    }
                });
        // 5. 绑定端口 + 异步监听启动结果
        serverChannel = serverBootstrap.bind(port).addListener(future -> {
            if (future.isSuccess()) {
                log.info("Netty server started on port {}", port);
            } else {
                log.error("Failed to start netty server", future.cause());
            }
        }).sync().channel();
    }

    @PreDestroy
    public void shutdown() {
        // 关闭服务端通道
        if (serverChannel != null) {
            serverChannel.close();
        }
        // 优雅关闭线程组，释放端口/资源
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        log.info("Netty server shutdown gracefully");
    }

}
