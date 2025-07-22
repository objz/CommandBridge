package dev.objz.commandbridge.paper.utils;


import dev.objz.commandbridge.core.utils.ConfigManager;
import dev.objz.commandbridge.core.utils.ScriptManager.Command;
import dev.objz.commandbridge.core.utils.ScriptManager.ScriptConfig;
import dev.objz.commandbridge.paper.core.Runtime;
import dev.objz.commandbridge.paper.Main;
import dev.objz.commandbridge.paper.Main.ServerInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class Encoder {
    private final Map<String, ScriptConfig> scripts = new LinkedHashMap<>();

    public void addScriptConfig(ScriptConfig config) {
        if (config == null || config.getName() == null) {
            throw new IllegalArgumentException("ScriptConfig or name cannot be null");
        }
        scripts.put(config.getName(), config);
    }

    public String encode() {
        StringBuilder sb = new StringBuilder();
        ServerInfo serverInfo = Main.detectServer();
        if (serverInfo == null) {
            throw new IllegalStateException("Server information could not be detected");
        }

        // header
        sb.append(serverInfo.name()).append("/").append(System.getProperty("java.version")).append("/")
                .append(serverInfo.version()).append("/").append(Main.getVersion());

        ConfigManager configManager = Runtime.getInstance().getConfig();
        sb.append("&");
        sb.append(configManager.getKey("config.yml", "client-id")).append(","); // server-id
        sb.append(configManager.getKey("config.yml", "debug")).append(","); // debug mode
        sb.append(configManager.getKey("config.yml", "remote")).append(","); // timeout
        sb.append(configManager.getKey("config.yml", "port")).append(","); // port
        sb.append(configManager.getKey("config.yml", "timeout")); // timeout

        for (ScriptConfig script : scripts.values()) {
            sb.append("@");

            // name
            sb.append(encodeField(script.getName())).append(",");

            // aliases
            sb.append(encodeList(script.getAliases())).append(",");

            // script flags: bit2=enabled, bit1=ignorePerm, bit0=hideWarn
            int scriptFlags = (script.isEnabled() ? 1 << 2 : 0)
                    | (script.shouldIgnorePermissionCheck() ? 1 << 1 : 0)
                    | (script.shouldHidePermissionWarning() ? 1 << 0 : 0);
            sb.append(Integer.toString(scriptFlags, 36));

            // commands
            for (Command cmd : script.getCommands()) {
                sb.append(";");
                // command
                sb.append(encodeField(cmd.getCommand())).append(",");
                // delay
                sb.append(cmd.getDelay()).append(",");
                // executor: 'p' or 'c'
                sb.append(cmd.getTargetExecutor().charAt(0)).append(",");
                // check-if-executor-is-player flag (bit0)
                int cFlag = cmd.isCheckIfExecutorIsPlayer() ? 1 : 0;
                sb.append(Integer.toString(cFlag, 36));
            }
        }

        return sb.toString();
    }

    public String compress(String input) {
        byte[] raw = input.getBytes(StandardCharsets.UTF_8);
        byte[] packed = gzip(raw);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(packed);
    }


    private byte[] gzip(byte[] data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gos      = new GZIPOutputStream(baos)) {
            gos.write(data);
            gos.finish();  // ensure all data is compressed
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("GZIP compression failed", e);
        }
    }



    // "." for null/empty.
    private String encodeField(String value) {
        if (value == null || value.isEmpty())
            return ".";
        return value.replace(",", "\\,").replace(";", "\\;");
    }

    // * join with '|' or return "." if empty
    private String encodeList(List<String> list) {
        if (list == null || list.isEmpty())
            return ".";
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            sb.append(encodeField(it.next()));
            if (it.hasNext())
                sb.append("|");
        }
        return sb.toString();
    }
}
