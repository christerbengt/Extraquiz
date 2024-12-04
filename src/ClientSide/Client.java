package ClientSide;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client extends JFrame {
    // Network components
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // GUI components
    private JLabel questionLabel;
    private JButton[] answerButtons;
    private JLabel resultLabel;

    public Client(String host, int port) {
        super("Quiz Game");
        setupGUI();
        connectToServer(host, port);
    }

    // Creates and arranges all GUI components
    private void setupGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        questionLabel = new JLabel("Waiting for other player...", SwingConstants.CENTER);
        add(questionLabel, BorderLayout.NORTH);

        // Panel for answer buttons
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        answerButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            answerButtons[i] = new JButton("Option " + (i + 1));
            int finalI = i;
            answerButtons[i].addActionListener(e -> sendAnswer(finalI));
            buttonsPanel.add(answerButtons[i]);
        }
        add(buttonsPanel, BorderLayout.CENTER);

        // Result display at the bottom
        resultLabel = new JLabel("", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(resultLabel, BorderLayout.SOUTH);

        setSize(400, 300);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void connectToServer(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a thread for receiving messages from server
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        String finalLine = line;
                        // Update GUI on Event Dispatch Thread
                        SwingUtilities.invokeLater(() -> processServerMessage(finalLine));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not connect to server");
        }
    }

    // Processes messages from server and updates GUI
    private void processServerMessage(String message) {
        System.out.println("Received: " + message);
        if (message.startsWith("QUESTION:")) {
            questionLabel.setText(message.substring(9));
        } else if (message.startsWith("OPTION:")) {
            // Find first button with default text and update it
            for (int i = 0; i < answerButtons.length; i++) {
                if (answerButtons[i].getText().startsWith("Option")) {
                    answerButtons[i].setText(message.substring(7));
                    break;
                }
            }
        } else if (message.startsWith("RESULT:")) {
            resultLabel.setText(message.substring(7));

            // Disable buttons after the game ends
            for (JButton button : answerButtons) {
                button.setEnabled(false);
            }
        }
    }

    private void sendAnswer(int answer) {
        out.println(answer);
        for (JButton button : answerButtons) {
            button.setEnabled(false);
        }
        questionLabel.setText("Waiting for other player to answer...");
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> new Client("localhost", 55555));
    }
}