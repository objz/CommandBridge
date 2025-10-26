package dev.objz.commandbridge.net.payloads.cmd;

import java.util.List;
import java.util.Optional;

public record InvokedCommand(
		String name,
		Optional<List<Object>> args,
		Object context
) {
}
