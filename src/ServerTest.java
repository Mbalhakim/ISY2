import java.io.BufferedReader;
import java.io.InputStreamReader;
import network.ServerClient;
import network.ServerListener;

public class ServerTest {
    public static void main(String[] args) {
        ServerClient client = new ServerClient();
        
        client.addListener(new ServerListener() {
            @Override
            public void onMessage(String message) {
                System.out.println("[SERVER RESPONSE] " + message);
            }
            
            @Override
            public void onError(Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
            
            @Override
            public void onClose() {
                System.out.println("[CONNECTION CLOSED]");
            }
        });
        
        try {
            System.out.println("=== SERVER TEST UTILITY ===");
            System.out.println("Connecting to localhost:7789...");
            client.connect("127.0.0.1", 7789);
            Thread.sleep(500);
            
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            
            // Login first
            System.out.print("Enter username: ");
            String username = br.readLine();
            System.out.println("\n[SENDING] login " + username);
            client.send("login " + username);
            Thread.sleep(500);
            
            // Get game list
            System.out.println("\n[SENDING] get gamelist");
            client.send("get gamelist");
            Thread.sleep(1000);
            
            // Get player list
            System.out.println("\n[SENDING] get playerlist");
            client.send("get playerlist");
            Thread.sleep(1000);
            
            // Get general help
            System.out.println("\n[SENDING] help");
            client.send("help");
            Thread.sleep(2000);
            
            // Get help for specific commands
            String[] commands = {"login", "subscribe", "move", "get", "challenge", "forfeit"};
            for (String cmd : commands) {
                System.out.println("\n[SENDING] help " + cmd);
                client.send("help " + cmd);
                Thread.sleep(1000);
            }
            
            // Interactive mode
            System.out.println("\n\n=== INTERACTIVE MODE ===");
            System.out.println("Type commands to send to server (or 'quit' to exit):");
            
            String input;
            while (true) {
                System.out.print("> ");
                input = br.readLine();
                
                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    break;
                }
                
                System.out.println("[SENDING] " + input);
                client.send(input);
                Thread.sleep(500);
            }
            
            client.close();
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
