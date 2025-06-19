package dev.objz.commandbridge.core.websocket;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.json.JSONException;
import org.json.JSONObject;

import dev.objz.commandbridge.core.json.MessageBuilder;
import dev.objz.commandbridge.core.json.MessageParser;
import dev.objz.commandbridge.core.Logger;
import dev.objz.commandbridge.core.utils.TLSUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslHandler;

public abstract class WebSocketClient {

    private Channel channel;
    private EventLoopGroup group;
    private final Logger logger;
    private final String secret;
    private URI uri;
    private WebSocketClientHandshaker handshaker;

    public WebSocketClient(Logger logger, String secret) {
        this.logger = logger;
        this.secret = secret;
    }

    public void connect(String address, int port) {
        try {
            this.uri = new URI("wss://" + address + ":" + port);
            group = new NioEventLoopGroup();

            final SSLContext sslContext = TLSUtils.createClientSSLContext();
            if (sslContext == null) {
                throw new RuntimeException("Failed to initialize SSL context");
            }
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            SSLEngine sslEngine = sslContext.createSSLEngine();
                            sslEngine.setUseClientMode(true);

                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new SslHandler(sslEngine));
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(8192));
                            pipeline.addLast(new WebSocketClientHandler());
                        }
                    });

            handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                    uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders());

            logger.info("Attempting to connect to server at: {}:{}", address, port);
            ChannelFuture future = bootstrap.connect(uri.getHost(), port).sync();
            channel = future.channel();

        } catch (Exception e) {
            throw new RuntimeException("Connection failed", e);
        }
    }

    public void disconnect() {
        if (channel != null && channel.isActive()) {
            try {
                channel.writeAndFlush(new CloseWebSocketFrame());
                channel.closeFuture().await(5, TimeUnit.SECONDS);
                logger.info("Disconnected successfully");
            } catch (Exception e) {
                throw new RuntimeException("Failed to disconnect WebSocket client", e);
            } finally {
                group.shutdownGracefully();
            }
        } else {
            logger.warn("Client is not connected, so no need to disconnect");
        }
    }

    public void sendMessage(JSONObject message) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(message.toString()));
        } else {
            logger.warn("Client is not connected, so cannot send message");
        }
    }

    private void handleMessage(String message) {
        try {
            MessageParser parser = new MessageParser(message);
            if (parser.getType().equals("auth")) {
                String status = parser.getStatus();

                switch (status) {
                    case "authenticated" -> {
                        logger.info("Authentication succeeded");
                        afterAuth();
                    }
                    case "unauthenticated" -> {
                        logger.error("Authentication failed");
                        channel.close();
                    }
                    case "error" ->
                        logger.warn("Received error from server: {}", parser.getBodyValueAsString("message"));
                    case null, default -> logger.error("Received not a valid status");
                }

            } else {
                onMessage(message);
            }
        } catch (JSONException e) {
            logger.error("Failed to parse message: {}", logger.getDebug() ? e : e.getMessage());
        }
    }

    protected abstract void onMessage(String message);

    protected abstract void afterAuth();

    private class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            handshaker.handshake(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.info("WebSocket Client disconnected!");
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel ch = ctx.channel();

            if (!handshaker.isHandshakeComplete()) {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                logger.info("Connected to server: {}", uri);

                MessageBuilder builder = new MessageBuilder("auth");
                builder.addToBody("secret", secret);
                JSONObject authMessage = builder.build();
                ch.writeAndFlush(new TextWebSocketFrame(authMessage.toString()));
                return;
            }

            if (msg instanceof TextWebSocketFrame) {
                TextWebSocketFrame textFrame = (TextWebSocketFrame) msg;
                handleMessage(textFrame.text());
            } else if (msg instanceof CloseWebSocketFrame) {
                ch.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("An error occurred: {}", logger.getDebug() ? cause : cause.getMessage());
            ctx.close();
        }
    }
}
