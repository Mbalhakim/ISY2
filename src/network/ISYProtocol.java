package network;

import java.util.HashMap;
import java.util.Map;

// Very small parser: recognizes lines like "SVR GAME MOVE {PLAYER: \"name\", MOVE: \"3\"}" and
// extracts type "SVR GAME MOVE" and a simple payload map for items inside { }
public class ISYProtocol {
    public static ISYMessage parse(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String trimmed = line.trim();
        String type = trimmed;
        Map<String, String> payload = new HashMap<>();
        int braceIdx = trimmed.indexOf('{');
        if (braceIdx >= 0) {
            type = trimmed.substring(0, braceIdx).trim();
            int endBrace = trimmed.indexOf('}', braceIdx);
            if (endBrace > braceIdx) {
                String inside = trimmed.substring(braceIdx + 1, endBrace).trim();
                // split by commas that are not inside quotes - naive but works for simple payloads
                String[] parts = inside.split(",");
                for (String p : parts) {
                    String[] kv = p.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replaceAll("\"", "");
                        String value = kv[1].trim().replaceAll("\"", "");
                        payload.put(key, value);
                    }
                }
            }
        }
        return new ISYMessage(type, payload);
    }

    public static String serialize(String cmd) {
        // client sends simple commands as-is
        return cmd + "\n";
    }
}

