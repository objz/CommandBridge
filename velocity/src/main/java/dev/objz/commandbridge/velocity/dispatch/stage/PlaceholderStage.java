package dev.objz.commandbridge.velocity.dispatch.stage;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.scripting.platform.PlatformFeatureKeys;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionResult;
import dev.objz.commandbridge.velocity.dispatch.model.Pipeline;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.william278.papiproxybridge.api.PlaceholderAPI;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final PlatformFeatures platformFeatures;

    public PlaceholderStage(PlatformFeatures platformFeatures) {
        this.platformFeatures = Objects.requireNonNull(platformFeatures);
    }

    @Override
    public void process(ExecutionContext context, Consumer<ExecutionResult> next) {
        CmdMapping currentCmd = context.currentCommand();
        if (currentCmd == null) {
            next.accept(ExecutionResult.ok(context));
            return;
        }

        String rawCommand = currentCmd.command();
        List<IdMapping> rawExecute = currentCmd.execute();

        boolean commandHasPlaceholder = rawCommand != null && !rawCommand.isBlank()
                && PATTERN.matcher(rawCommand).find();
        boolean executeHasPlaceholder = rawExecute != null && rawExecute.stream()
                .anyMatch(t -> t != null && t.id() != null && PATTERN.matcher(t.id()).find());

        if (!commandHasPlaceholder && !executeHasPlaceholder) {
            next.accept(ExecutionResult.ok(context));
            return;
        }

        Map<String, Object> args = Optional.ofNullable(context.arguments()).orElse(Map.of());

        String resolvedCommand = commandHasPlaceholder ? substitute(rawCommand, args) : rawCommand;
        List<IdMapping> resolvedExecute = executeHasPlaceholder
                ? rawExecute.stream()
                        .map(t -> new IdMapping(substitute(t.id(), args), t.location()))
                        .toList()
                : rawExecute;

        if (commandHasPlaceholder) {
            Log.debug("Placeholder resolution: '{}' -> '{}'", rawCommand, resolvedCommand);
        }
        if (executeHasPlaceholder) {
            Log.debug("Execute target resolution: '{}' -> '{}'", rawExecute, resolvedExecute);
        }

        Optional<UUID> papiUuid = commandHasPlaceholder ? parsePAPI(context.source()) : Optional.empty();

        CompletionStage<String> commandStage;

        if (papiUuid.isPresent()) {
            PlaceholderAPI papi = PlaceholderAPI.createInstance();
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
                    resolvedExecute,
                    currentCmd.server(),
                    currentCmd.delay(),
                    currentCmd.cooldown());

            next.accept(ExecutionResult.ok(
                    context.nextCommand(resolvedCmd, context.commandIndex())));
        });
    }

    private String substitute(String input, Map<String, Object> args) {
        Matcher matcher = PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (key == null || key.isBlank()) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement("${}"));
                continue;
            }

            Object value = args.get(key);
            String replacement = value == null ? "" : convertToString(value);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Optional<UUID> parsePAPI(CommandSource source) {
        if (!platformFeatures.isEnabled(Location.VELOCITY, PlatformFeatureKeys.PAPI)) {
            return Optional.empty();
        }

        if (source instanceof Player player) {
            return Optional.of(player.getUniqueId());
        }
        return Optional.empty();
    }

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
