package com.bank.app.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;
import java.util.Objects;

public abstract class NettyServerBuilder {
    private final ServerBootstrap bootstrap = NettyServer.newDefaultBootstrap();

    private final String name;

    private boolean localAddressSet;

    protected NettyServerBuilder(String name) {
        this.name = name;
    }

    final String name() {
        return name;
    }

    public NettyServerBuilder group(int parentGroupThreads, int childGroupThreads) {
        bootstrap.group(new NioEventLoopGroup(parentGroupThreads), new NioEventLoopGroup(childGroupThreads));
        return this;
    }

    public NettyServerBuilder localAddress(InetSocketAddress localAddress) {
        bootstrap.localAddress(Objects.requireNonNull(localAddress, "localAddress"));
        localAddressSet = true;
        return this;
    }

    public NettyServerBuilder localAddress(String host, int port) {
        bootstrap.localAddress(host, port);
        localAddressSet = true;
        return this;
    }

    ServerBootstrap getBootstrap() {
        InnerUtils.checkState(localAddressSet, "localAddress not set");

        ChannelHandler initializer = getServerPipeline();
        return bootstrap
                .handler(new LoggingHandler(name() + "-server"))
                .childHandler(initializer);
    }

    abstract ChannelHandler getServerPipeline();

    public NettyServer build() {
        ServerBootstrap bootstrap = getBootstrap();
        return new NettyServer(name, bootstrap);
    }
}
