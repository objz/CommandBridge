package dev.objz.commandbridge.api.channel;

/**
 * Marker interface for all data types that can be sent over a
 * {@link dev.objz.commandbridge.api.channel.MessageChannel}.
 *
 * <p>
 * Implementing this interface registers a type as a valid payload. The
 * {@link dev.objz.commandbridge.api.channel.MessageChannel} uses the payload's
 * {@code Class} token for channel routing and type-safe dispatch.
 *
 * <p>
 * {@link dev.objz.commandbridge.api.channel.command.CommandPayload} is the
 * built-in implementation for command execution. Custom payload types can be
 * created by implementing this interface:
 *
 * <pre>{@code
 * public record MyPayload(String data) implements ChannelPayload {
 * }
 * }</pre>
 *
 * @see dev.objz.commandbridge.api.channel.command.CommandPayload
 * @see dev.objz.commandbridge.api.channel.MessageChannel
 */
public interface ChannelPayload {
}
