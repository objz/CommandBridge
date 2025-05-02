package dev.consti.commandbridge.velocity.websocket;

import dev.consti.foundationlib.logging.Logger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.ssl.NotSslRecordException;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import javax.net.ssl.SSLHandshakeException;


@Sharable
public class HttpServer extends SimpleChannelInboundHandler<FullHttpRequest>{
    private Logger logger;

    
    public HttpServer(Logger logger) {
        this.logger = logger;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        String uri = msg.uri();
        logger.debug("Incoming HTTP request to: {}", uri);

        if ("/ping".equalsIgnoreCase(uri)) {
            sendTextResponse(ctx, OK, "pong");
        } else if ("websocket".equalsIgnoreCase(msg.headers().get(HttpHeaderNames.UPGRADE))) {
            ctx.fireChannelRead(msg.retain());
        } else {
            sendTextResponse(ctx, NOT_FOUND, "Not Found");
        }
    }

    private void sendTextResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HTTP_1_1,
            status,
            ctx.alloc().buffer().writeBytes(content.getBytes())
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof NotSslRecordException || cause instanceof SSLHandshakeException) {
            if (logger.getDebug()) {
                logger.debug("SSL handshake or protocol error", cause);
            } else {
                logger.warn("Received invalid or unsupported SSL/TLS connection (enable debug for full trace)");
            }
        } else {
            if (logger.getDebug()) {
                logger.error("Unexpected error in HttpServer handler", cause);
            } else {
                logger.error("Unexpected error in HttpServer handler: {}", cause.getMessage());
            }
        }
        ctx.close();
    }

}
