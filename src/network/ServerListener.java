package network;

public interface ServerListener {
    void onMessage(String message);
    void onError(Exception e);
    void onClose();
}
