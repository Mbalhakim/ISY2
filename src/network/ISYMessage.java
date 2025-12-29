package network;

import java.util.Map;

public class ISYMessage {
    private final String type;
    private final Map<String, String> payload;

    public ISYMessage(String type, Map<String, String> payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "ISYMessage{" + "type='" + type + '\'' + ", payload=" + payload + '}';
    }
}
