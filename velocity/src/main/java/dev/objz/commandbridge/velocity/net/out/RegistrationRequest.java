package dev.objz.commandbridge.velocity.net.out;

import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.logging.Summary;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.payloads.cmd.RegisterCommands;
import dev.objz.commandbridge.net.payloads.feedback.Feedback;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.velocity.net.out.ctx.RegistrationRequestContext;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public final class RegistrationRequest extends OutboundHandler<RegistrationRequestContext> {

    @Override
    public SendOperation accept(RegistrationRequestContext ctx) {
        String clientId = ctx.session.id();

        List<CommandStub> stubs = ctx.scripts.stream().map(s -> {
            try {
                return new CommandStub(
                        s.name(),
                        s.aliases(),
                        s.description(),
                        s.args());
            } catch (Exception e) {
                Log.error(e, "Stub export failed for '{}'", s.name());
                return null;
            }
        }).filter(Objects::nonNull).collect(toList());

        if (stubs.isEmpty()) {
            Log.warn("No valid command stubs for '{}'", clientId);
            if (ctx.resultCallback != null) {
                ctx.resultCallback.accept(false);
            }
            throw new IllegalStateException("no stubs");
        }

        var payload = new RegisterCommands(stubs);
        ObjectNode payloadNode = Envelope.MAPPER.valueToTree(payload);
        final Envelope env = Envelope.make(MessageType.REGISTER_COMMANDS, serverId, clientId, payloadNode);

        SendOperation op = send(ctx.session.ch(), env)
                .expect(MessageType.REGISTER_COMMANDS_RESULT)
                .timeout(ctx.timeout);

        op.await().thenApply(feedbackEnv -> {
            if (feedbackEnv != null) {
                try {
                    Feedback feedback = Envelope.MAPPER.treeToValue(
                            feedbackEnv.payload(),
                            Feedback.class);

                    Summary.feedbackSummary("Feedback", feedback, clientId);
                    Summary.feedbackDetails(feedback, clientId, false);

                    if (ctx.resultCallback != null) {
                        ctx.resultCallback.accept(feedback.succeeded() > 0);
                    }

                } catch (Exception e) {
                    Log.error(e, "Failed to process registration feedback from '{}'",
                            clientId);
                    if (ctx.resultCallback != null) {
                        ctx.resultCallback.accept(false);
                    }
                }
            } else {
                if (ctx.resultCallback != null) {
                    ctx.resultCallback.accept(false);
                }
            }
            return env;
        }).exceptionally(ex -> {
            var cause = (ex.getCause() != null) ? ex.getCause() : ex;
            if (cause instanceof java.util.concurrent.TimeoutException) {
                Log.error("Timeout from '{}' after {}", clientId,
                        ctx.timeout.toString());
            } else {
                Log.error(cause, "Failed to receive feedback from '{}'", clientId);
            }
            if (ctx.resultCallback != null) {
                ctx.resultCallback.accept(false);
            }
            return null;
        });

        return op;

    }
}
