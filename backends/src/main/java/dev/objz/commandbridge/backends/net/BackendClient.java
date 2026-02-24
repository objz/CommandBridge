package dev.objz.commandbridge.backends.net;

import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.scripting.model.enums.Location;

public interface BackendClient extends AutoCloseable {
    void start() throws Exception;

    void reconnect() throws Exception;

    void scheduleReconnection();

    SendOperation send(Envelope request);

    ClientStatus status();

    String serverId();

    InNode inboundRouter();

    OutNode<Object> outboundRouter();

    void setLocation(Location location);

    void setServerId(String serverId);

    @Override
    void close() throws Exception;
}
