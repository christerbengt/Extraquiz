package ServerSide;

import java.io.*;
import java.net.*;
import java.util.*;

// Server class handles game logic and client connections
public class Server implements Runnable {
    private ServerSocket serverSocket; // Accepts incoming client connections
    private volatile boolean running = true; // Server lifecycle control
    private List<ClientHandler> clients = new ArrayList<>(); // Stores connected clients

    // Game data, hardcoded since we only have one question. With more questions we would probably add a separate
    // Question class to handle them
    private final String QUESTION = "Vilken stad är residensstad i Halland?";
    private final String[] OPTIONS = {"Borås", "Halmstad", "Varberg", "Kungsbacka"};
    private final int CORRECT_ANSWER = 1; // Halmstad är residensstad

    // The constructor initializes server socket on the specified port
    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    // Main server loop - accepts client connections until two players have joined.
    @Override
    public void run() {
        System.out.println("Server started, waiting for 2 players...");
        try {
            // Keep accepting connections until we have two players
            while (running && clients.size() < 2) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, clients.size());
                clients.add(handler);
                new Thread(handler).start(); // Start handling client in new thread
                System.out.println("Player " + clients.size() + " connected");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Inner class to handle client connections for separation of concerns,
    // Server accepts new connections while ClientHandler manages
    // communication with a specific client, i.e. answers and such.
    // Each instance of ClientHandler represents one connected client and
    //runs on its own thread (thanks to implementing Runnable).
    private class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private int playerNumber;
        private Integer answer;

        public ClientHandler(Socket socket, int playerNumber) throws IOException {
            this.socket = socket;
            this.playerNumber = playerNumber;
            // Set up input/output streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            try {

                // Send question and options to client
                out.println("QUESTION:" + QUESTION);
                for (String option : OPTIONS) {
                    out.println("OPTION:" + option);
                }

                // Wait for answer from client and process it
                String answerStr = in.readLine();
                if (answerStr != null) {
                    answer = Integer.parseInt(answerStr);
                    System.out.println("Player " + playerNumber + " answered: " + answer);

                    // This prevents threads from accessing allPlayersAnswered at the  exact same time
                    synchronized (clients) {
                        if (allPlayersAnswered()) {
                            sendResults();
                        }
                    }
                }

                // Keep connection open until game ends
                while (running && !socket.isClosed()) {
                    Thread.sleep(100);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // Checks if both players have submitted their answers
        private boolean allPlayersAnswered() {
            return clients.size() == 2 && clients.stream().allMatch(c -> c.answer != null);
        }

        private void sendResults() {
            boolean player1Correct = clients.get(0).answer == CORRECT_ANSWER;
            boolean player2Correct = clients.get(1).answer == CORRECT_ANSWER;

            if (player1Correct == player2Correct) {
                clients.forEach(c -> c.out.println("RESULT:LIKA"));
            } else {
                clients.get(0).out.println("RESULT:" + (player1Correct ? "DU VANN!" : "DU FÖRLORADE"));
                clients.get(1).out.println("RESULT:" + (player2Correct ? "DU VANN!" : "DU FÖRLORADE"));
            }
            running = false; // End the game after sending results
        }
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server(55555);
        new Thread(server).start();
    }
}