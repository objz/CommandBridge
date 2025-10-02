package dev.objz.commandbridge.main.scripting.v3.compiler.compilers;

import dev.objz.commandbridge.main.scripting.v3.compiler.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compiler.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compiler.io.Validators;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Field;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Schema;
import dev.objz.commandbridge.main.scripting.v3.model.domain.CommandStep;
import dev.objz.commandbridge.main.scripting.v3.model.domain.TargetKind;
import dev.objz.commandbridge.main.scripting.v3.model.domain.TargetServer;
import dev.objz.commandbridge.main.scripting.v3.model.dto.CommandStepDto;
import dev.objz.commandbridge.main.scripting.v3.model.dto.TargetKindDto;
import dev.objz.commandbridge.main.scripting.v3.model.dto.TargetServerDto;

public final class CommandStepCompiler implements NodeCompiler<CommandStepDto, CommandStep> {
	@Override
	public CommandStep compile(CommandStepDto raw, CompileContext ctx, ProblemSink p, Path path) {
		var cmd = new Field<>("command", Schema.STEP_COMMAND).required(raw.command, p, path);

		TargetKind kind = ctx.compile(TargetKindDto.class, raw.kind, p, path.child("kind"));
		TargetServer server = ctx.compile(TargetServerDto.class, raw.server, p, path.child("server"));

		var delay = new Field<>("delay", Schema.STEP_DELAY).orDefault(raw.delay);
		var timeout = new Field<>("timeout", Schema.STEP_TIMEOUT).orDefault(raw.timeout);

		Validators.nonNegative(delay, path, p, "delay");
		Validators.positive(timeout, path, p, "timeout");

		return new CommandStep(cmd, kind, server, delay, timeout);
	}
}
