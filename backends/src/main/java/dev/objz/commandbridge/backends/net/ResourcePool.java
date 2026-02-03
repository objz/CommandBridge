package dev.objz.commandbridge.backends.net;

import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.security.TlsResolver;
import dev.objz.commandbridge.security.TrustManager;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.ssl.JsseXnioSsl;
import org.xnio.ssl.XnioSsl;

import javax.net.ssl.SSLContext;
import java.util.concurrent.TimeUnit;

public final class ResourcePool implements AutoCloseable {
    private XnioWorker worker;
    private ByteBufferPool bufferPool;
    private XnioSsl ssl;
    private final ClassLoader classLoader;

    public ResourcePool() {
        this.classLoader = getClass().getClassLoader();
    }

    public synchronized void initialize() throws Exception {
        if (worker != null) {
            Log.warn("ResourcePool already initialized, skipping");
            return;
        }

        Xnio xnio = Xnio.getInstance("nio", classLoader);
        this.worker = xnio.createWorker(OptionMap.EMPTY);

        this.bufferPool = new DefaultByteBufferPool(
                /* direct */ false,
                /* bufferSize */ 16 * 1024,
                /* maximumPoolSize */ -1,
                /* threadLocalCacheSize */ 4,
                /* leakDetectionPercent */ 10);

        Log.info("ResourcePool initialized (worker={}, bufferPool={})",
                worker != null, bufferPool != null);
    }

    public synchronized void initializeSsl(TlsMode mode, String configuredPin, boolean isReconnecting)
            throws Exception {
        if (!TlsResolver.isTlsEnabled(mode)) {
            this.ssl = null;
            return;
        }

        if (worker == null) {
            throw new IllegalStateException("Worker must be initialized before SSL");
        }

        TrustManager tm;
        try {
            tm = new TrustManager(mode, configuredPin);
        } catch (Exception e) {
            Log.error(e, "Failed to create TrustManager for TLS mode {}", mode);
            throw e;
        }

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(/* keyManagers */ null, new javax.net.ssl.TrustManager[] { tm }, /* random */ null);

        this.ssl = new JsseXnioSsl(worker.getXnio(), OptionMap.EMPTY, ctx);

        if (!isReconnecting && mode == TlsMode.TOFU) {
            if (configuredPin != null && !configuredPin.isBlank()) {
                Log.debug("TLS mode=TOFU with configured pin (will verify)");
            } else {
                Log.debug("TLS mode=TOFU without configured pin (will auto pin in memory)");
            }
        }
    }

    public XnioWorker getWorker() {
        return worker;
    }

    public ByteBufferPool getBufferPool() {
        return bufferPool;
    }

    public XnioSsl getSsl() {
        return ssl;
    }

    public boolean isInitialized() {
        return worker != null && bufferPool != null;
    }

    @Override
    public synchronized void close() {
        Log.debug("Closing ResourcePool");

        ssl = null;

        if (worker != null) {
            try {
                worker.shutdown();
            } catch (Throwable t) {
                Log.warn("Error shutting down worker: {}", t.getMessage());
            }

            try {
                boolean terminated = worker.awaitTermination(5, TimeUnit.SECONDS);
                if (!terminated) {
                    Log.warn("Worker did not terminate within 5 seconds");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.warn("Interrupted while waiting for worker termination");
            } catch (Throwable t) {
                Log.warn("Error waiting for worker termination: {}", t.getMessage());
            }

            worker = null;
        }

        if (bufferPool != null) {
            bufferPool = null;
        }

        Log.debug("ResourcePool closed");
    }
}
