package dev.objz.commandbridge.velocity.dispatch.stage;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.velocity.Main;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionResult;
import dev.objz.commandbridge.velocity.dispatch.model.Pipeline;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.william278.papiproxybridge.api.PlaceholderAPI;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

public final class PlaceholderStage implements Pipeline {

    private static final Pattern PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private final PlaceholderAPI papi = PlaceholderAPI.createInstance();

    @Override
    public void process(ExecutionContext context, Consumer<ExecutionResult> next) {
        CmdMapping currentCmd = context.currentCommand();
        if (currentCmd == null) {
            next.accept(ExecutionResult.ok(context));
            return;
        }

        String rawCommand = currentCmd.command();
        if (rawCommand == null || rawCommand.isBlank()) {
            next.accept(ExecutionResult.ok(context));
            return;
        }

        Matcher checkMatcher = PATTERN.matcher(rawCommand);
        if (!checkMatcher.find()) {
            next.accept(ExecutionResult.ok(context));
            return;
        }

        Map<String, Object> args = context.arguments();
        if (args == null) {
            args = Map.of();
        }

        Matcher matcher = PATTERN.matcher(rawCommand);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            if (key == null || key.isBlank()) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement("${}"));
                continue;
            }

            Object value = args.get(key);
            String replacement;

            if (value == null) {
                replacement = "";
            } else {
                replacement = convertToString(value);
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        String resolvedCommand = sb.toString();

        // re validate the parsed string now with papi placeholder

        Optional<UUID> papiUuid = parsePAPI(context.source());

        CompletionStage<String> commandStage;

        if (papiUuid.isPresent()) {
            commandStage = papi.formatPlaceholders(resolvedCommand, papiUuid.get())
                    .handle((papiResolved, throwable) -> {
                        if (throwable != null) {
                            Log.error(
                                    "PlaceholderAPI parsing failed: {}", throwable.getMessage());

                            context.source().sendMessage(
                                    Component.text(
                                            "PlaceholderAPI parsing failed. Please check the console",
                                            NamedTextColor.RED));

                            return resolvedCommand;
                        }
                        return papiResolved;
                    });
        } else {
            // no PAPI
            commandStage = CompletableFuture.completedFuture(resolvedCommand);
        }

        commandStage.thenAccept(finalCommand -> {

            CmdMapping resolvedCmd = new CmdMapping(
                    finalCommand,
                    currentCmd.runAs(),
                    currentCmd.execute(),
                    currentCmd.server(),
                    currentCmd.delay(),
                    currentCmd.cooldown());

            next.accept(ExecutionResult.ok(
                    context.nextCommand(resolvedCmd, context.commandIndex())));
        });
    }

    private Optional<UUID> parsePAPI(CommandSource source) {
        if (!Main.isPapiEnabled) {
            return Optional.empty();
        }

        if (source instanceof Player) {
            UUID pUuid = ((Player) source).getUniqueId();
            return Optional.of(pUuid);
        }
        return Optional.empty();

    };

    private String convertToString(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof java.util.Collection<?> collection) {
            if (collection.isEmpty()) {
                return "";
            }
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (Object item : collection) {
                if (!first) {
                    result.append(" ");
                }
                first = false;
                result.append(itemToString(item));
            }
            return result.toString();
        }

        return String.valueOf(value);
    }

    private String itemToString(Object item) {
        if (item == null) {
            return "";
        }

        if (item instanceof dev.objz.commandbridge.cmd.ref.EntityRef ref) {
            return ref.name() != null ? ref.name() : ref.uuid();
        }

        if (item instanceof dev.objz.commandbridge.cmd.ref.Location3D loc) {
            return loc.x() + " " + loc.y() + " " + loc.z();
        }

        if (item instanceof dev.objz.commandbridge.cmd.ref.Location2D loc) {
            return loc.x() + " " + loc.y();
        }

        return String.valueOf(item);
    }
}
