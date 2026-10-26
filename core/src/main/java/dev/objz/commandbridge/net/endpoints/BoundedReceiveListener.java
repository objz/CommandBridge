package dev.objz.commandbridge.net.endpoints;

import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.UTF8Output;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;

import java.io.IOException;

public abstract class BoundedReceiveListener extends AbstractReceiveListener {
    private static final int MAX_MESSAGE_BYTES = 4 * 1024 * 1024;

    @Override
    protected final long getMaxTextBufferSize() {
        return MAX_MESSAGE_BYTES;
    }

    @Override
    protected final long getMaxBinaryBufferSize() {
        return MAX_MESSAGE_BYTES;
    }

    @Override
    protected final void onText(WebSocketChannel channel, StreamSourceFrameChannel frame) {
        // Undertow's async text reader misses size accounting; bound raw bytes before decoding.
        new BufferedBinaryMessage(MAX_MESSAGE_BYTES, true).read(frame, new WebSocketCallback<BufferedBinaryMessage>() {
            @Override
            public void complete(WebSocketChannel ch, BufferedBinaryMessage message) {
                try (var data = message.getData()) {
                    onTextMessage(ch, new UTF8Output(data.getResource()).extract());
                } catch (IOException e) {
                    BoundedReceiveListener.this.onError(ch, e);
                }
            }

            @Override
            public void onError(WebSocketChannel ch, BufferedBinaryMessage message, Throwable error) {
                try {
                    message.getData().close();
                } finally {
                    BoundedReceiveListener.this.onError(ch, error);
                }
            }
        });
    }

    protected abstract void onTextMessage(WebSocketChannel channel, String message) throws IOException;
}
