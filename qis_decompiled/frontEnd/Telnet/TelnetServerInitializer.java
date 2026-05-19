/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelInitializer
 *  io.netty.channel.ChannelPipeline
 *  io.netty.channel.socket.SocketChannel
 *  io.netty.handler.codec.DelimiterBasedFrameDecoder
 *  io.netty.handler.codec.Delimiters
 *  io.netty.handler.codec.bytes.ByteArrayEncoder
 *  io.netty.handler.codec.string.StringDecoder
 *  io.netty.handler.codec.string.StringEncoder
 *  io.netty.handler.ssl.SslContext
 */
package frontEnd.Telnet;

import frontEnd.Telnet.TelnetServerHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import src.com.quarch.deviceInterface.DeviceList;

public class TelnetServerInitializer
extends ChannelInitializer<SocketChannel> {
    private static final StringDecoder DECODER = new StringDecoder();
    private static final StringEncoder ENCODER = new StringEncoder();
    private static final ByteArrayEncoder bENCODER = new ByteArrayEncoder();
    private TelnetServerHandler SERVER_HANDLER;
    private final SslContext sslCtx;
    static DeviceList deviceList;

    public TelnetServerInitializer(SslContext sslCtx, DeviceList deviceList) {
        this.sslCtx = sslCtx;
        TelnetServerInitializer.deviceList = deviceList;
        this.SERVER_HANDLER = new TelnetServerHandler(deviceList);
    }

    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (this.sslCtx != null) {
            pipeline.addLast(new ChannelHandler[]{this.sslCtx.newHandler(ch.alloc())});
        }
        pipeline.addLast(new ChannelHandler[]{new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter())});
        pipeline.addLast(new ChannelHandler[]{DECODER});
        pipeline.addLast(new ChannelHandler[]{bENCODER});
        pipeline.addLast(new ChannelHandler[]{ENCODER});
        pipeline.addLast(new ChannelHandler[]{this.SERVER_HANDLER});
    }
}

