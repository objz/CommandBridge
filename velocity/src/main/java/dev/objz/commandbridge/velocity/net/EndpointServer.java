package dev.objz.commandbridge.velocity.net;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;

public interface EndpointServer {
    void start();

    void stop();

    SendOperation send(Endpoint endpoint, Envelope request);

    void close(Endpoint endpoint);
}
