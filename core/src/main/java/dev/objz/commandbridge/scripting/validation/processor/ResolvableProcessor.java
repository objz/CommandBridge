package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.scripting.anno.Resolvable;
import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.validation.PostProcessor;

import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ResolvableProcessor implements PostProcessor {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]*)}");

    @Override
    public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
        perFieldSyntaxOnly(buf, ctx);

        if (Script.class.equals(buf.recordClass())) {
            validateCommandsUseKnownArgs(buf, ctx);
        }
    }

    private static void perFieldSyntaxOnly(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
        RecordComponent[] comps = buf.components();
        for (int i = 0; i < comps.length; i++) {
            Resolvable ann = comps[i].getAnnotation(Resolvable.class);
            if (ann == null)
                continue;

            Object v = buf.get(i);
            if (!(v instanceof CharSequence cs))
                continue;

            final String field = comps[i].getName();
            final String s = cs.toString();

            Matcher m = PLACEHOLDER.matcher(s);
            while (m.find()) {
                String content = m.group(1); // inside ${...}
                if (content == null || content.isBlank()) {
                    ctx.problems().error(field, "Empty placeholder ${} is not allowed");
                }
            }
        }
    }

    private static void validateCommandsUseKnownArgs(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
        Set<String> allowed = new HashSet<>();
        int idxArgs = -1, idxCommands = -1;

        RecordComponent[] comps = buf.components();
        for (int i = 0; i < comps.length; i++) {
            String n = comps[i].getName();
            if ("args".equals(n))
                idxArgs = i;
            else if ("commands".equals(n))
                idxCommands = i;
        }
        if (idxArgs >= 0) {
            Object raw = buf.get(idxArgs);
            if (raw instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof ArgMapping a) {
                        String name = a.name();
                        if (name != null && !name.isBlank()) {
                            allowed.add(name);
                        }
                    }
                }
            }
        }

        if (idxCommands < 0)
            return;
        Object rawCmds = buf.get(idxCommands);
        if (!(rawCmds instanceof List<?> cmds))
            return;

        for (int cmdIndex = 0; cmdIndex < cmds.size(); cmdIndex++) {
            Object o = cmds.get(cmdIndex);
            if (!(o instanceof CmdMapping cmd))
                continue;

            String template = cmd.command();
            if (template == null)
                continue;

            Matcher m = PLACEHOLDER.matcher(template);
            int occurrence = 0;
            while (m.find()) {
                String content = m.group(1);
                if (content == null || content.isBlank()) {
                    occurrence++;
                    continue;
                }

                if (!allowed.contains(content)) {
                    ctx.problems().error(
                            "command[" + cmdIndex + "]",
                            "Unknown argument '" + content + "' at placeholder["
                                    + occurrence + "]");
                }
                occurrence++;
            }
        }
    }
}
