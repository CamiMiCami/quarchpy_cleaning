/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.Unpooled
 *  io.netty.channel.ChannelFutureListener
 *  io.netty.channel.ChannelHandler$Sharable
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.ChannelInboundHandlerAdapter
 *  io.netty.handler.codec.http.DefaultFullHttpResponse
 *  io.netty.handler.codec.http.HttpRequest
 *  io.netty.handler.codec.http.HttpResponseStatus
 *  io.netty.handler.codec.http.HttpVersion
 *  io.netty.util.concurrent.GenericFutureListener
 */
package frontEnd.Rest;

import commandProcessor.CmdProcessorSingleton;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.List;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.deviceInterface.DeviceList;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.utils.DebugUtil;

@ChannelHandler.Sharable
public class RESTServerHandler
extends ChannelInboundHandlerAdapter {
    static DeviceList deviceList;
    private static CmdProcessorSingleton cmdProcessor;
    private static final String CONTENT_TYPE;
    private static final String CONTENT_LENGTH;
    private static final String CONNECTION;
    private static final String KEEP_ALIVE;
    private List<String> fullHTTPRequest;
    private DeviceListEntry defaultDevice = null;

    public RESTServerHandler(DeviceList deviceList) {
        RESTServerHandler.deviceList = deviceList;
        cmdProcessor = CmdProcessorSingleton.getInstance();
    }

    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        HttpRequest req = null;
        DefaultFullHttpResponse response = null;
        if (!(msg instanceof HttpRequest)) {
            return;
        }
        req = (HttpRequest)msg;
        String command = req.uri().substring(1);
        command = command.replace("%20", " ").trim();
        CmdStruct cmdStruct = new CmdStruct();
        boolean close = false;
        cmdStruct.command = command;
        DebugUtil.debugErrorln(command);
        if (cmdStruct.command.startsWith("CheckPortPermission?")) {
            cmdStruct.response.add("OK");
        } else if (cmdStruct.command.startsWith("CheckAuthorization")) {
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer((byte[])"OK".getBytes()));
        } else {
            cmdStruct.setDefaultDevice(this.defaultDevice);
            cmdProcessor.cmdDecode(cmdStruct);
            if (cmdStruct.getDefaultDevice() != null) {
                this.defaultDevice = cmdStruct.getDefaultDevice();
            } else if (cmdStruct.getDefaultDevice() == null && !cmdStruct.defaultDeviceIsValid) {
                this.defaultDevice = null;
            }
        }
        boolean keepAlive = false;
        if (response == null) {
            StringBuilder sb;
            if (cmdStruct.getStringBuilder() != null) {
                sb = cmdStruct.getStringBuilder();
            } else {
                sb = new StringBuilder();
                for (String s : cmdStruct.response) {
                    if (s.startsWith(">")) {
                        sb.append(s);
                        continue;
                    }
                    sb.append(s + "\r\n");
                }
            }
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer((byte[])sb.toString().getBytes()));
            response.headers().set(CONTENT_TYPE, (Object)"text/plain");
            response.headers().set(CONTENT_LENGTH, (Object)response.content().readableBytes());
        }
        if (!keepAlive) {
            ctx.write((Object)response).addListener((GenericFutureListener)ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(CONNECTION, (Object)KEEP_ALIVE);
            ctx.write((Object)response);
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    static {
        CONTENT_TYPE = new String("Content-Type");
        CONTENT_LENGTH = new String("Content-Length");
        CONNECTION = new String("Connection");
        KEEP_ALIVE = new String("keep-alive");
    }
}

