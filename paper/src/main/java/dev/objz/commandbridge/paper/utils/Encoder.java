package dev.objz.commandbridge.paper.utils;

import com.github.luben.zstd.Zstd;
import dev.objz.commandbridge.core.utils.ScriptManager.Command;
import dev.objz.commandbridge.core.utils.ScriptManager.ScriptConfig;
import dev.objz.commandbridge.paper.Main;
import dev.objz.commandbridge.paper.Main.ServerInfo;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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


        //header
        sb.append(serverInfo.name()).append("/").append(System.getProperty("java.version")).append("/").append(serverInfo.version());

        for (ScriptConfig script : scripts.values()) {
            sb.append("@");

            //name
            sb.append(encodeField(script.getName())).append(",");

            //aliases
            sb.append(encodeList(script.getAliases())).append(",");

            //script flags: bit2=enabled, bit1=ignorePerm, bit0=hideWarn
            int scriptFlags =
                  (script.isEnabled()                    ? 1 << 2 : 0)
                | (script.shouldIgnorePermissionCheck()? 1 << 1 : 0)
                | (script.shouldHidePermissionWarning()? 1 << 0 : 0);
            sb.append(Integer.toString(scriptFlags, 36));

            //commands
            for (Command cmd : script.getCommands()) {
                sb.append(";");
                //command 
                sb.append(encodeField(cmd.getCommand())).append(",");
                //delay
                sb.append(cmd.getDelay()).append(",");
                //executor: 'p' or 'c'
                sb.append(cmd.getTargetExecutor().charAt(0)).append(",");
                //check-if-executor-is-player flag (bit0)
                int cFlag = cmd.isCheckIfExecutorIsPlayer() ? 1 : 0;
                sb.append(Integer.toString(cFlag, 36));
            }
        }

        return sb.toString();
    }


    public String compress(String input) {
        byte[] in = input.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = Zstd.compress(in, 22);
        return Base64.getUrlEncoder()
                     .withoutPadding()
                     .encodeToString(compressed);
    }

    //"." for null/empty.
    private String encodeField(String value) {
        if (value == null || value.isEmpty()) return ".";
        return value.replace(",", "\\,").replace(";", "\\;");
    }

    //* join with '|' or return "." if empty
    private String encodeList(List<String> list) {
        if (list == null || list.isEmpty()) return ".";
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            sb.append(encodeField(it.next()));
            if (it.hasNext()) sb.append("|");
        }
        return sb.toString();
    }
}
