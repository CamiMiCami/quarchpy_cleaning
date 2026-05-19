/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.Unpooled
 *  io.netty.channel.ChannelFuture
 *  io.netty.channel.ChannelFutureListener
 *  io.netty.channel.ChannelHandler$Sharable
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.SimpleChannelInboundHandler
 *  io.netty.util.concurrent.GenericFutureListener
 */
package frontEnd.Telnet;

import appBase.AppVersion;
import commandProcessor.CmdProcessorSingleton;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.GenericFutureListener;
import java.nio.ByteBuffer;
import java.util.Date;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.deviceInterface.DeviceList;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.devices.IFCustomManagedBuffer;
import src.com.quarch.utils.DebugUtil;

@ChannelHandler.Sharable
public class TelnetServerHandler
extends SimpleChannelInboundHandler<String> {
    static DeviceList deviceList;
    private static CmdProcessorSingleton cmdProcessor;
    private DeviceListEntry defaultDevice = null;
    private IFCustomManagedBuffer cmBufferToReturn;

    public TelnetServerHandler(DeviceList deviceList) {
        TelnetServerHandler.deviceList = deviceList;
        cmdProcessor = CmdProcessorSingleton.getInstance();
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.write((Object)("$t " + AppVersion.getQualifiedAppVersion() + " " + new Date() + "\r\n"));
        ctx.flush();
    }

    void debugPrintDWords(byte[] b, int start, int len) {
        for (int i = start; i < start + len; ++i) {
            System.out.print(Integer.toString(b[i]) + " ");
            System.out.print(Integer.toString(b[i + 1]) + " ");
            System.out.print(Integer.toString(b[i + 2]) + " ");
            System.out.println(Integer.toString(b[i + 3]) + " ");
        }
    }

    public void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
        ChannelFuture future;
        CmdStruct cmdStruct = new CmdStruct();
        this.cmBufferToReturn = null;
        boolean close = false;
        cmdStruct.command = request;
        if (DebugUtil.isEnableDebug()) {
            System.out.println(DebugUtil.dateStr() + "Telnet: Before command " + request);
        }
        long startTime = System.nanoTime();
        cmdStruct.setDefaultDevice(this.defaultDevice);
        cmdProcessor.cmdDecode(cmdStruct);
        if (cmdStruct.getDefaultDevice() != null) {
            this.defaultDevice = cmdStruct.getDefaultDevice();
        } else if (cmdStruct.getDefaultDevice() == null && !cmdStruct.defaultDeviceIsValid) {
            this.defaultDevice = null;
        }
        if (DebugUtil.isEnableDebug()) {
            System.out.println("Telnet: After command " + request + "(" + String.valueOf((System.nanoTime() - startTime) / 1000L) + "uS)");
            int lines = cmdStruct.response.size();
            if (lines > 0) {
                System.out.println("Reply :" + (String)cmdStruct.response.get(0) + " (" + lines + ") Lines inc. Cursor");
            }
        }
        if (cmdStruct.action == -1) {
            close = true;
        }
        if (cmdStruct.bArray == null && cmdStruct.getCmBuffer() == null) {
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
            future = ctx.writeAndFlush((Object)sb.toString());
        } else {
            if (cmdStruct.getCmBuffer() != null) {
                this.cmBufferToReturn = cmdStruct.getCmBuffer();
                ByteBuffer bBuffer = this.cmBufferToReturn.getUnderlyingBuffer();
                DebugUtil.devDebugMsgln(System.currentTimeMillis() + " Telnet: Unpooled.wrappedBuffer " + bBuffer.limit() + "bytes");
                future = ctx.writeAndFlush((Object)Unpooled.wrappedBuffer((ByteBuffer)bBuffer));
            } else {
                future = ctx.writeAndFlush((Object)cmdStruct.bArray);
            }
            future.addListener((GenericFutureListener)new ChannelFutureListener(){

                public void operationComplete(ChannelFuture future) {
                    if (TelnetServerHandler.this.cmBufferToReturn != null) {
                        DebugUtil.devDebugMsgln(System.currentTimeMillis() + " cmBufferToReturn.freeBuffer()");
                        TelnetServerHandler.this.cmBufferToReturn.freeBuffer();
                        TelnetServerHandler.this.cmBufferToReturn = null;
                    }
                }
            });
        }
        if (close) {
            future.addListener((GenericFutureListener)ChannelFutureListener.CLOSE);
        }
    }

    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("userEventTriggered");
    }

    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    }

    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}

