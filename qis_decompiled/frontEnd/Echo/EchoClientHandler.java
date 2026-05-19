/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.Unpooled
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.ChannelInboundHandlerAdapter
 */
package frontEnd.Echo;

import frontEnd.Echo.EchoClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class EchoClientHandler
extends ChannelInboundHandlerAdapter {
    private final ByteBuf firstMessage = Unpooled.buffer((int)EchoClient.SIZE);

    public EchoClientHandler() {
        for (int i = 0; i < this.firstMessage.capacity(); ++i) {
            this.firstMessage.writeByte((int)((byte)i));
        }
    }

    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush((Object)this.firstMessage);
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ctx.write(msg);
    }

    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}

