package Os_QuizGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;

public class QuizClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JFrame frame;
    private JTextArea textArea;
    private JTextField answerField;
    private JButton sendButton;

    public QuizClient(String serverAddress) {
        setupGUI();
        connectToServer(serverAddress);
    }

    private void setupGUI() {
        frame = new JFrame("Quiz Client");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        answerField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener((@SuppressWarnings("unused") ActionEvent e) -> sendAnswer());

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(answerField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void connectToServer(String serverAddress) {
        try {
            socket = new Socket(serverAddress, 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(this::listenToServer).start();
        } catch (IOException e) {
            textArea.append("Unable to connect to server.\n");
        }
    }

    private void listenToServer() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.equals("ENTER_NAME")) {
                    String name = JOptionPane.showInputDialog("Enter your name:");
                    out.println(name);
                } else if (msg.startsWith("QUESTION:")) {
                    textArea.append("\n" + msg.substring(9) + "\n");
                } else if (msg.equals("WRONG_ANSWER")) {
                    textArea.append("Your answer was incorrect. Try again!\n");
                } else if (msg.startsWith("CORRECT:")) {
                    String[] parts = msg.split(":");
                    textArea.append(parts[1] + " got the point in " + parts[2] + " seconds!\n");
                } else if (msg.startsWith("SCORES:")) {
                    String[] parts = msg.split(":");
                    textArea.append("Final Score: " + parts[1] + " points, Time: " + parts[2] + " sec\n");
                }
            }
        } catch (IOException e) {
            textArea.append("Disconnected from server.\n");
        }
    }

    private void sendAnswer() {
        out.println("ANSWER:" + answerField.getText());
        answerField.setText("");
    }

    public static void main(String[] args) {
        String serverIP = JOptionPane.showInputDialog("Enter Server IP:");
        new QuizClient(serverIP);
    }
}
