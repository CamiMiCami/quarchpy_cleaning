/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  QuarchLogging.QuarchLoggerInterface
 *  io.netty.bootstrap.ServerBootstrap
 *  io.netty.channel.ChannelFuture
 *  io.netty.channel.ChannelFutureListener
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.EventLoopGroup
 *  io.netty.channel.nio.NioEventLoopGroup
 *  io.netty.channel.socket.nio.NioServerSocketChannel
 *  io.netty.handler.ssl.SslContext
 *  io.netty.handler.ssl.SslContextBuilder
 *  io.netty.handler.ssl.util.SelfSignedCertificate
 *  io.netty.util.concurrent.GenericFutureListener
 */
package frontEnd.Telnet;

import QuarchLogging.QuarchLoggerInterface;
import frontEnd.Telnet.TelnetServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.File;
import java.net.BindException;
import java.util.logging.Level;
import properties.AppProperties;
import src.com.quarch.deviceInterface.DeviceList;
import src.com.quarch.utils.DebugUtil;

public final class TelnetServer
implements Runnable,
QuarchLoggerInterface {
    static final boolean SSL = System.getProperty("ssl") != null;
    static int PORT = 9722;
    static DeviceList deviceList;
    public static volatile boolean initComplete;
    public static volatile boolean running;

    public TelnetServer(DeviceList deviceList) {
        TelnetServer.deviceList = deviceList;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void xXmain(String[] args) throws Exception {
        SslContext sslCtx;
        DebugUtil.debugMsgln("Telnet: 1");
        try {
            AppProperties.getInstance();
            PORT = AppProperties.appPropertiesData.getLocalPorts().getTelnetInt();
        }
        catch (Exception e) {
            StackTraceElement[] st = e.getStackTrace();
            DebugUtil.debugMsgln("Telnet: 1 Exception:");
            for (StackTraceElement ste : st) {
                DebugUtil.debugMsgln("\t" + ste.toString());
            }
        }
        DebugUtil.debugMsgln("Telnet: 1.1");
        if (PORT == -1) {
            DebugUtil.debugMsgln("Telnet: 1.2");
            initComplete = true;
            return;
        }
        DebugUtil.debugMsgln("Telnet: 2");
        QuarchLoggerInterface.logToDefault((String)"TelnetServer", (Level)Level.INFO, (String)(" Starting on port " + PORT));
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer((File)ssc.certificate(), (File)ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }
        DebugUtil.debugMsgln("Telnet: 3");
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            ((ServerBootstrap)b.group((EventLoopGroup)bossGroup, (EventLoopGroup)workerGroup).channel(NioServerSocketChannel.class)).childHandler((ChannelHandler)new TelnetServerInitializer(sslCtx, deviceList));
            DebugUtil.debugMsgln("Telnet: 4");
            b.bind(PORT).addListener((GenericFutureListener)new ChannelFutureListener(){

                public void operationComplete(ChannelFuture future) throws Exception {
                    initComplete = true;
                    if (!future.isSuccess()) {
                        DebugUtil.debugMsgln("Telnet Bind Error:" + future.cause().getMessage());
                        if (future.cause() instanceof BindException) {
                            // empty if block
                        }
                    } else {
                        running = true;
                    }
                }
            }).sync().channel().closeFuture().sync();
            DebugUtil.debugMsgln("Telnet: 5");
        }
        catch (Exception e) {
            DebugUtil.debugMsgln("Telnet Exception:" + e.getMessage());
        }
        finally {
            DebugUtil.debugMsgln("Telnet: end");
            running = false;
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void run() {
        try {
            TelnetServer.xXmain(null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static {
        initComplete = false;
        running = false;
    }
}

