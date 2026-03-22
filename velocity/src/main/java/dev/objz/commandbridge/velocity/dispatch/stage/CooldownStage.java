package dev.objz.commandbridge.velocity.dispatch.stage;

import com.velocitypowered.api.proxy.Player;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionResult;
import dev.objz.commandbridge.velocity.dispatch.model.Pipeline;
import dev.objz.commandbridge.velocity.util.CooldownManager;
import dev.objz.commandbridge.util.MM;

import java.time.Duration;
import java.util.function.Consumer;

public final class CooldownStage implements Pipeline {

    private final CooldownManager cooldownManager;

    public CooldownStage(CooldownManager cooldownManager) {
        this.cooldownManager = cooldownManager;
    }

    @Override
    public void process(ExecutionContext context, Consumer<ExecutionResult> next) {
        Script script = context.script();

        if (!(context.source() instanceof Player player)) {
            next.accept(ExecutionResult.ok(context));
            return;
        }

        if (cooldownManager.isOnCooldown(script.name(), player.getUniqueId())) {
            Duration remaining = cooldownManager.getRemaining(script.name(), player.getUniqueId());
            context.source().sendMessage(
                    MM.error("Try again in " + MM.formatDuration(remaining)));
            next.accept(ExecutionResult.stop("Cooldown active"));
            return;
        }

        next.accept(ExecutionResult.ok(context));
    }
}
