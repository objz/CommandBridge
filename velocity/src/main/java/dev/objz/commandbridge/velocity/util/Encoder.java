package dev.objz.commandbridge.velocity.util;

import com.github.luben.zstd.Zstd;
import dev.objz.commandbridge.core.utils.ScriptManager.Command;
import dev.objz.commandbridge.core.utils.ScriptManager.ScriptConfig;
import dev.objz.commandbridge.velocity.Main;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Encoder {
    private final Map<String, ScriptConfig> scripts = new LinkedHashMap<>();

    private final Map<String, String> clientsScripts = new HashMap<>();

    public Map<String, String> getClientsScripts() {
        return new LinkedHashMap<>(clientsScripts);
    }

    public void clearClientsScripts() {
        clientsScripts.clear();
    }

    public void addScriptConfig(ScriptConfig config) {
        if (config == null || config.getName() == null) {
            throw new IllegalArgumentException("ScriptConfig or name cannot be null");
        }
        scripts.put(config.getName(), config);
    }

    public String encode() {
        StringBuilder sb = new StringBuilder();

        //header
        sb.append("V").append("/").append(System.getProperty("java.version")).append("/").append(Main.getInstance().getVelocityVersion()).append("/").append(Main.getVersion());

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
                sb.append(encodeField(cmd.getCommand())).append(",");                  // command
                sb.append(cmd.getDelay()).append(",");                                 // delay
                sb.append(encodeList(cmd.getTargetClientIds())).append(",");           // targets
                sb.append(cmd.getTargetExecutor().charAt(0)).append(",");              // executor: 'p'/'c'

                //command flags: bit2=waitOnline, bit1=checkPlayer, bit0=checkOnServer
                int cmdFlags =
                      (cmd.shouldWaitUntilPlayerIsOnline() ? 1 << 2 : 0)
                    | (cmd.isCheckIfExecutorIsPlayer()    ? 1 << 1 : 0)
                    | (cmd.isCheckIfExecutorIsOnServer()  ? 1 << 0 : 0);
                sb.append(Integer.toString(cmdFlags, 36));
            }
        }

        for (Map.Entry<String, String> entry : clientsScripts.entrySet()) {
            sb.append("#");
            sb.append(encodeField(entry.getKey())).append(","); // clientId
            sb.append(encodeField(entry.getValue()));           // encoded string
        }

        return sb.toString();
    }

    public void addClient(String clientId, String encodedString) {
        if (clientId == null || clientId.isEmpty() || encodedString == null || encodedString.isEmpty()) {
            throw new IllegalArgumentException("Client ID and encoded string cannot be null or empty");
        }
        clientsScripts.put(clientId, encodedString);
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
