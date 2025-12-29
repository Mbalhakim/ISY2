package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ISYClient {
    private final List<ISYListener> listeners = new ArrayList<>();
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void addListener(ISYListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    public void removeListener(ISYListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        running.set(true);
        executor.submit(this::readLoop);
    }

    private void readLoop() {
        try {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                notifyRaw(line);
                try {
                    ISYMessage msg = ISYProtocol.parse(line);
                    if (msg != null) notifyMessage(msg);
                } catch (Exception e) {
                    notifyError(e);
                }
            }
        } catch (SocketException se) {
            notifyError(se);
        } catch (IOException e) {
            notifyError(e);
        } finally {
            closeInternal();
            notifyClose();
        }
    }

    public void sendRaw(String cmd) {
        if (writer != null) {
            writer.print(ISYProtocol.serialize(cmd));
            writer.flush();
        }
    }

    public void close() {
        running.set(false);
        closeInternal();
        executor.shutdownNow();
    }

    private void closeInternal() {
        try { if (reader != null) reader.close(); } catch (IOException ignored) {}
        try { if (writer != null) writer.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        reader = null; writer = null; socket = null;
    }

    private void notifyMessage(ISYMessage msg) {
        synchronized (listeners) {
            for (ISYListener l : listeners) {
                try { l.onMessage(msg); } catch (Exception ignored) {}
            }
        }
    }
    private void notifyRaw(String line) {
        synchronized (listeners) {
            for (ISYListener l : listeners) {
                try { l.onRawLine(line); } catch (Exception ignored) {}
            }
        }
    }
    private void notifyError(Exception e) {
        synchronized (listeners) {
            for (ISYListener l : listeners) {
                try { l.onError(e); } catch (Exception ignored) {}
            }
        }
    }
    private void notifyClose() {
        synchronized (listeners) {
            for (ISYListener l : listeners) {
                try { l.onClose(); } catch (Exception ignored) {}
            }
        }
    }
}

