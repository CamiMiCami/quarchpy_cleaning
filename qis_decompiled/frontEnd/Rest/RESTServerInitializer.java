/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelInitializer
 *  io.netty.channel.ChannelPipeline
 *  io.netty.channel.socket.SocketChannel
 *  io.netty.handler.codec.http.HttpServerCodec
 *  io.netty.handler.codec.string.StringDecoder
 *  io.netty.handler.codec.string.StringEncoder
 *  io.netty.handler.ssl.SslContext
 */
package frontEnd.Rest;

import frontEnd.Rest.RESTServerHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import src.com.quarch.deviceInterface.DeviceList;

public class RESTServerInitializer
extends ChannelInitializer<SocketChannel> {
    private static final StringDecoder DECODER = new StringDecoder();
    private static final StringEncoder ENCODER = new StringEncoder();
    private RESTServerHandler SERVER_HANDLER;
    private final SslContext sslCtx;
    static DeviceList deviceList;

    public RESTServerInitializer(SslContext sslCtx, DeviceList deviceList) {
        this.sslCtx = sslCtx;
        RESTServerInitializer.deviceList = deviceList;
        this.SERVER_HANDLER = new RESTServerHandler(deviceList);
    }

    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (this.sslCtx != null) {
            p.addLast(new ChannelHandler[]{this.sslCtx.newHandler(ch.alloc())});
        }
        p.addLast(new ChannelHandler[]{new HttpServerCodec()});
        p.addLast(new ChannelHandler[]{this.SERVER_HANDLER});
    }
}

