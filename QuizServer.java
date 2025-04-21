import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class QuizServer {
    private ServerSocket serverSocket;
    private ArrayList<ClientHandler> clients = new ArrayList<>();
    private String[][] questions = {
            {"What is the capital of Japan?", "Tokyo"},
            {"What is 5 + 7?", "12"},
            {"Who developed Java?", "James Gosling"}
    };
    private int currentQuestion = 0;
    private boolean gameStarted = false;
    private boolean questionAnswered = false;
    private long questionStartTime;

    private JFrame frame;
    private JTextArea textArea;
    private JButton startButton;

    public QuizServer() {
        setupGUI();
        startServer();
    }

    @SuppressWarnings("unused")
    private void setupGUI() {
        frame = new JFrame("Quiz Server");
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        startButton = new JButton("Start Quiz");
        startButton.addActionListener(e -> startGame());

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(startButton, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(12345);
                textArea.append("Server started on port 12345\nWaiting for players to join...\n");

                while (true) {
                    Socket socket = serverSocket.accept();
                    if (gameStarted) {
                        socket.close();
                        continue;
                    }
                    ClientHandler client = new ClientHandler(socket);
                    clients.add(client);
                    new Thread(client).start();
                    textArea.append("New player joined!\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startGame() {
        if (clients.isEmpty()) {
            textArea.append("No players connected!\n");
            return;
        }

        gameStarted = true;
        startButton.setEnabled(false);
        textArea.append("Game Started!\n");

        for (ClientHandler client : clients) {
            client.sendMessage("START");
        }

        askQuestion();
    }

    private void askQuestion() {
        if (currentQuestion >= questions.length) {
            endGame();
            return;
        }

        String question = questions[currentQuestion][0];
        textArea.append("\nQuestion: " + question + "\n");

        questionStartTime = System.currentTimeMillis();
        questionAnswered = false;

        for (ClientHandler client : clients) {
            client.sendMessage("QUESTION:" + question);
        }
    }

    private void handleAnswer(String answer, ClientHandler sender) {
        if (questionAnswered) {
            return;
        }

        if (answer.equalsIgnoreCase(questions[currentQuestion][1])) {
            long timeTaken = (System.currentTimeMillis() - questionStartTime) / 1000;
            sender.score++;
            sender.totalTime += timeTaken;
            questionAnswered = true;

            textArea.append(sender.name + " answered correctly in " + timeTaken + " seconds and gets a point!\n");

            for (ClientHandler client : clients) {
                client.sendMessage("CORRECT:" + sender.name + ":" + timeTaken);
            }

            currentQuestion++;
            askQuestion();
        } else {
            sender.sendMessage("WRONG_ANSWER");
        }
    }

    private void endGame() {
        textArea.append("\nGame Over!\nFinal Scores:\n");

        for (ClientHandler client : clients) {
            textArea.append(client.name + ": " + client.score + " points, Time Taken: " + client.totalTime + " sec\n");
            client.sendMessage("SCORES:" + client.score + ":" + client.totalTime);
        }
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String name;
        private int score = 0;
        private long totalTime = 0;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String msg) {
            out.println(msg);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("ENTER_NAME");
                name = in.readLine();
                textArea.append(name + " has joined the game.\n");

                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.startsWith("ANSWER:")) {
                        handleAnswer(msg.substring(7), this);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new QuizServer();
    }
}