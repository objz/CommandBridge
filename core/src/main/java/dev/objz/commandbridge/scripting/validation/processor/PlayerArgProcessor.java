package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.scripting.anno.PlayerResolvable;
import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.model.Defaults;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.Server;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.validation.PostProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.*;

public final class PlayerArgProcessor implements PostProcessor {

    @Override
    public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
        if (!Script.class.equals(buf.recordClass()))
            return;

        int idxArgs = -1, idxDefaults = -1, idxCommands = -1;
        RecordComponent[] comps = buf.components();

        for (int i = 0; i < comps.length; i++) {
            String n = comps[i].getName();
            if ("args".equals(n))
                idxArgs = i;
            else if ("defaults".equals(n))
                idxDefaults = i;
            else if ("commands".equals(n))
                idxCommands = i;
        }

        if (idxArgs < 0)
            return;

        Map<String, ArgMapping> argsByName = new HashMap<>();
        Object rawArgs = buf.get(idxArgs);
        if (rawArgs instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof ArgMapping a && a.name() != null && !a.name().isBlank()) {
                    argsByName.put(a.name(), a);
                }
            }
        }

        String defaultsPlayerArg = null;

        if (idxDefaults >= 0) {
            Object raw = buf.get(idxDefaults);
            if (raw instanceof Defaults defaults && defaults.server() != null) {
                defaultsPlayerArg = defaults.server().playerArg();
                validatePlayerArg(defaults.server(), argsByName, "defaults.server", ctx);
            }
        }

        if (idxCommands >= 0) {
            Object rawCmds = buf.get(idxCommands);
            if (rawCmds instanceof List<?> cmds) {
                for (int i = 0; i < cmds.size(); i++) {
                    if (!(cmds.get(i) instanceof CmdMapping cmd) || cmd.server() == null)
                        continue;

                    String cmdPlayerArg = cmd.server().playerArg();
                    if (Objects.equals(cmdPlayerArg, defaultsPlayerArg))
                        continue;

                    validatePlayerArg(cmd.server(), argsByName,
                            "command[" + i + "].server", ctx);
                }
            }
        }
    }

    private void validatePlayerArg(Server server, Map<String, ArgMapping> argsByName,
            String path, BindContext ctx) {
        String playerArg = server.playerArg();
        if (playerArg == null || playerArg.isBlank())
            return;

        ArgMapping arg = argsByName.get(playerArg);
        if (arg == null) {
            ctx.problems().error(path,
                    String.format("player-arg references unknown argument '%s'",
                            playerArg));
            return;
        }

        if (arg.type() == null)
            return;

        if (!isPlayerResolvable(arg.type())) {
            ctx.problems().error(path,
                    String.format("player-arg '%s' (type '%s') is not player-resolvable, "
                                    + "must be [STRING, PLAYERS]",
                            playerArg,
                            arg.type().name()));
        }
    }

    private boolean isPlayerResolvable(ArgType type) {
        try {
            Field field = ArgType.class.getField(type.name());
            return field.isAnnotationPresent(PlayerResolvable.class);
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
