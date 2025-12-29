package network;

public interface ISYListener {
    void onMessage(ISYMessage message);
    void onRawLine(String line);
    void onError(Exception e);
    void onClose();
}

