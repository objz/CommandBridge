package dev.objz.commandbridge.velocity.ws.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.main.logging.FeedbackLog;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.feedback.Feedback;
import dev.objz.commandbridge.velocity.ws.MessageRouter.InboundHandler;
import dev.objz.commandbridge.velocity.ws.SessionHub;
import io.undertow.websockets.core.WebSocketChannel;

public final class FeedbackHandler implements InboundHandler {
	private final ObjectMapper mapper;
	private final SessionHub sessions;

	public FeedbackHandler(ObjectMapper mapper, SessionHub sessions) {
		this.mapper = mapper;
		this.sessions = sessions;
	}

	@Override
	public void handle(WebSocketChannel ch, Envelope env) throws Exception {
		Feedback fb = mapper.treeToValue(env.payload(), Feedback.class);

		FeedbackLog.summary("Feedback", fb, env.from());
		FeedbackLog.details(fb, env.from());

		sessions.completeFeedback(env);
	}
}
