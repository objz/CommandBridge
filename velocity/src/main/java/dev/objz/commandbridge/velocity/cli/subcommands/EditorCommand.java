package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.editor.EditorLayoutStore;
import dev.objz.commandbridge.velocity.editor.EditorSession;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Report;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

public final class EditorCommand extends AbstractCliCommand {

    private final ScriptManager scriptManager;
    private final ReloadCommand reload;
    private final SessionHub sessionHub;
    private final String serverId;
    private final EditorLayoutStore layout;
    private volatile EditorSession active;

    public EditorCommand(ScriptManager scriptManager, ReloadCommand reload, SessionHub sessionHub,
            String serverId, Path dataDir) {
        this.scriptManager = scriptManager;
        this.reload = reload;
        this.sessionHub = sessionHub;
        this.serverId = serverId;
        this.layout = new EditorLayoutStore(dataDir);
    }

    public void open(CommandSource sender) {
        openFor(sender);
    }

    /** Opens a new editor session for {@code sender}, prints its link, and hands back the link. */
    public CompletableFuture<String> openFor(CommandSource sender) {
        EditorSession current = active;
        if (current != null && current.isOpen()) {
            current.close();
        }

        if (new RenderContext(sender).isPlayer()) {
            sender.sendMessage(MM.muted("Connecting to the editor relay..."));
        } else {
            Log.info("Connecting to the editor relay...");
        }
        return EditorSession.open(scriptManager, reload, sender, this::servers, layout, () -> active = null)
                .whenComplete((session, error) -> {
                    if (error != null) {
                        Report.of("Editor").error("Could not open editor: " + rootMessage(error)).send(sender);
                        return;
                    }
                    // two opens can overlap; the one that finishes last wins and the other is closed, not orphaned
                    EditorSession previous = active;
                    active = session;
                    if (previous != null && previous != session && previous.isOpen()) {
                        previous.close();
                    }
                    sendLink(sender, session, "Editor session opened");
                })
                .thenApply(EditorSession::url);
    }

    /** Saves the changes the web editor is waiting with; the code is shown in the editor after Save. */
    public void confirm(CommandSource sender, String code) {
        EditorSession current = active;
        if (current == null || !current.isOpen()) {
            Report.of("Editor").warn("No editor session is open").send(sender);
            return;
        }
        try {
            List<String> errors = current.confirm(code);
            if (errors.isEmpty()) {
                Report.of("Editor").success("Saved the editor's changes and reloaded the scripts").send(sender);
            } else {
                Report report = Report.of("Editor").error("Not everything was saved");
                errors.forEach(report::bullet);
                report.send(sender);
            }
        } catch (Exception e) {
            Report.of("Editor").error("Could not save: " + rootMessage(e)).send(sender);
        }
    }

    public void list(CommandSource sender) {
        EditorSession current = active;
        if (current == null || !current.isOpen()) {
            Report.of("Editor").muted("No editor session is open").send(sender);
            return;
        }
        Report.of("Editor").bullet(current.describe()).send(sender);
    }

    /** Closes the open session without a message, e.g. when CommandBridge restarts. */
    public void closeSession() {
        EditorSession current = active;
        if (current != null && current.isOpen()) {
            current.close();
        }
    }

    public void close(CommandSource sender) {
        EditorSession current = active;
        if (current == null || !current.isOpen()) {
            Report.of("Editor").warn("No editor session is open").send(sender);
            return;
        }
        current.close();
        Report.of("Editor").success("Editor session closed").send(sender);
    }

    private Map<String, Map<String, String>> servers() {
        Map<String, Map<String, String>> out = new TreeMap<>();
        out.put(serverId, Map.of("location", "VELOCITY", "platform", "VELOCITY"));
        for (ClientSession s : sessionHub.authenticated()) {
            out.put(s.id(), Map.of("location", s.location() != null ? s.location().name() : "BACKEND",
                    "platform", s.platform().name()));
        }
        return out;
    }

    private static void sendLink(CommandSource sender, EditorSession session, String message) {
        Report.of("Editor")
                .success(message)
                .link("Editor", session.url(), "Click to open")
                .muted("Anyone with this link can edit your scripts. Close it with /cb editor close")
                .send(sender);
    }

    private static String rootMessage(Throwable error) {
        Throwable t = error;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }
}
