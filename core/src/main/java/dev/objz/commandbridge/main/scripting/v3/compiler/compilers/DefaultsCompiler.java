package dev.objz.commandbridge.main.scripting.v3.compiler.compilers;

import dev.objz.commandbridge.main.scripting.v3.compiler.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compiler.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compiler.io.Validators;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Field;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Schema;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Defaults;
import dev.objz.commandbridge.main.scripting.v3.model.domain.TargetKind;
import dev.objz.commandbridge.main.scripting.v3.model.domain.TargetServer;
import dev.objz.commandbridge.main.scripting.v3.model.dto.DefaultsDto;
import dev.objz.commandbridge.main.scripting.v3.model.dto.TargetServerDto;

public final class DefaultsCompiler implements NodeCompiler<DefaultsDto, Defaults> {

    @Override
    public Defaults compile(DefaultsDto raw, CompileContext ctx, ProblemSink p, Path path) {
        String runAsStr = raw == null ? null : raw.runAs;
        Defaults.RunAs runAs = Validators.parseEnum(
                Defaults.RunAs.class, runAsStr, p, path, "run-as", Defaults.RunAs.CONSOLE);

        String id = raw == null ? null : raw.id;
        if (id == null || id.isBlank()) {
            p.error(path.child("id").toString(), "is required");
            id = "unknown";
        }

        TargetKind kind = new TargetKindCompiler(true)
                .compile(raw == null ? null : raw.kind, ctx, p, path.child("kind"));

        TargetServer server = ctx.compile(TargetServerDto.class,
                raw == null ? null : raw.server, p, path.child("server"));

        var delay = new Field<>("delay", Schema.DEFAULT_DELAY).orDefault(raw == null ? null : raw.delay);
        var cooldown = new Field<>("cooldown", Schema.DEFAULT_COOLDOWN).orDefault(raw == null ? null : raw.cooldown);

        Validators.nonNegative(delay, path, p, "delay");
        Validators.nonNegative(cooldown, path, p, "cooldown");

        return new Defaults(runAs, id, kind, server, delay, cooldown);
    }
}
