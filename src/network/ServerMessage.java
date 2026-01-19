package network;

import java.util.HashMap;
import java.util.Map;

public class ServerMessage {
    private String type;
    private Map<String, String> payload;

    public ServerMessage(String rawMessage) {
        this.payload = new HashMap<>();
        parse(rawMessage);
    }

    private void parse(String message) {
        if (message.startsWith("OK")) {
            type = "OK";
        } else if (message.startsWith("ERR")) {
            type = "ERR";
            int spaceIndex = message.indexOf(' ');
            if (spaceIndex > 0) {
                payload.put("message", message.substring(spaceIndex + 1).trim());
            }
        } else if (message.startsWith("SVR")) {
            parseServerMessage(message);
        } else {
            type = "UNKNOWN";
            payload.put("raw", message);
        }
    }

    private void parseServerMessage(String message) {
        // SVR GAME MATCH {...}
        // SVR GAME YOURTURN {...}
        // SVR GAME MOVE {...}
        // SVR GAME WIN/LOSS/DRAW {...}
        // SVR GAMELIST [...]
        // SVR PLAYERLIST [...]
        
        if (message.contains("MATCH")) {
            type = "MATCH";
            extractMapPayload(message);
        } else if (message.contains("YOURTURN")) {
            type = "YOURTURN";
            extractMapPayload(message);
        } else if (message.contains("MOVE")) {
            type = "MOVE";
            extractMapPayload(message);
        } else if (message.contains("WIN")) {
            type = "WIN";
            extractMapPayload(message);
        } else if (message.contains("LOSS")) {
            type = "LOSS";
            extractMapPayload(message);
        } else if (message.contains("DRAW")) {
            type = "DRAW";
            extractMapPayload(message);
        } else if (message.contains("CHALLENGE")) {
            type = "CHALLENGE";
            extractMapPayload(message);
        } else if (message.contains("GAMELIST")) {
            type = "GAMELIST";
            payload.put("raw", message);
        } else if (message.contains("PLAYERLIST")) {
            type = "PLAYERLIST";
            payload.put("raw", message);
        } else {
            type = "SERVER";
            payload.put("raw", message);
        }
    }

    private void extractMapPayload(String message) {
        int startIndex = message.indexOf('{');
        int endIndex = message.lastIndexOf('}');
        
        if (startIndex >= 0 && endIndex > startIndex) {
            String mapContent = message.substring(startIndex + 1, endIndex);
            String[] pairs = mapContent.split(",");
            
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim().replace("\"", "");
                    payload.put(key, value);
                }
            }
        }
    }

    public String getType() {
        return type;
    }

    public String get(String key) {
        return payload.get(key);
    }

    public Map<String, String> getPayload() {
        return payload;
    }
}
