package network;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerClient {
    private Socket socket;
    private BufferedReader reader;
    private java.io.OutputStream writer;
    private List<ServerListener> listeners;
    private Thread readerThread;
    private boolean running;

    public ServerClient() {
        this.listeners = new ArrayList<>();
        this.running = false;
    }

    public void addListener(ServerListener listener) {
        listeners.add(listener);
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        writer = socket.getOutputStream();
        running = true;
        
        readerThread = new Thread(this::readLoop);
        readerThread.start();
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                final String msg = line;
                for (ServerListener listener : listeners) {
                    listener.onMessage(msg);
                }
            }
        } catch (IOException e) {
            if (running) {
                for (ServerListener listener : listeners) {
                    listener.onError(e);
                }
            }
        } finally {
            for (ServerListener listener : listeners) {
                listener.onClose();
            }
        }
    }

    public void send(String message) {
        if (writer != null) {
            try {
                System.out.println("[SEND] " + message);
                String fullMessage = message + "\n";
                writer.write(fullMessage.getBytes("UTF-8"));
                writer.flush();
            } catch (Exception e) {
                System.out.println("[SEND ERROR] " + e.getMessage());
            }
        }
    }

    public void close() {
        running = false;
        try {
            if (socket != null) socket.close();
            if (reader != null) reader.close();
            if (writer != null) writer.close();
        } catch (IOException e) {
            // Ignore
        } 
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
