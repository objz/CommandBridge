package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.scripting.anno.Max;
import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.validation.PostProcessor;

import java.lang.reflect.RecordComponent;
import java.time.Duration;

public final class MaxProcessor implements PostProcessor {

    @Override
    public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
        RecordComponent[] comps = buf.components();
        for (int i = 0; i < comps.length; i++) {
            Max ann = comps[i].getAnnotation(Max.class);
            if (ann == null)
                continue;

            Object v = buf.get(i);
            if (v == null)
                continue;

            String field = comps[i].getName();
            long max = ann.value();

            if (v instanceof Number n) {
                long lv = n.longValue();
                if (lv > max) {
                    String msg = ann.message().isBlank() ? ("must be <= " + max) : ann.message();
                    ctx.problems().error(field, msg);
                }
                continue;
            }
            if (v instanceof Duration d) {
                if (d.compareTo(Duration.ofSeconds(max)) > 0) {
                    String msg = ann.message().isBlank() ? ("must be <= " + max + "s")
                            : ann.message();
                    ctx.problems().error(field, msg);
                }
                continue;
            }

            ctx.problems().error(field,
                    "@Max allowed only on numeric or Duration fields (got "
                            + v.getClass().getSimpleName() + ")");
        }
    }
}
