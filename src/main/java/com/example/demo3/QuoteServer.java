package com.example.demo3;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class QuoteServer {
    private static final int PORT = 12345;
    private static final String QUOTES_FILE = "src/main/resources/quotes.txt";
    private static final String USERS_FILE = "src/main/resources/users.json";

    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<String, Integer> videoVotes = new HashMap<>();
    private static final List<String> proposedVideos = new ArrayList<>();
    private static final List<String> quotes = new ArrayList<>();

    private static final Logger logger = Logger.getLogger(QuoteServer.class.getName());

    public static void main(String[] args) {
        configureLogger();
        logger.info("Starting QuoteServer...");
        loadQuotes();

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server started on port " + PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Client connected: " + clientSocket.getInetAddress());

                    ClientHandler handler = new ClientHandler(clientSocket);
                    synchronized (clients) {
                        clients.add(handler);
                    }
                    threadPool.submit(handler);
                } catch (IOException e) {
                    logger.warning("Error accepting client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.severe("Error starting server: " + e.getMessage());
        } finally {
            threadPool.shutdown();
            logger.info("Server shut down.");
        }
    }

    private static void loadQuotes() {
        try (BufferedReader reader = new BufferedReader(new FileReader(QUOTES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                quotes.add(line.trim());
            }
            logger.info("Loaded " + quotes.size() + " quotes.");
        } catch (IOException e) {
            logger.warning("Failed to load quotes: " + e.getMessage());
        }
    }

    private static void configureLogger() {
        try {
            LogManager.getLogManager().reset();
            Logger rootLogger = Logger.getLogger("");
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(new SimpleFormatter());
            rootLogger.addHandler(consoleHandler);

            FileHandler fileHandler = new FileHandler("server.log", true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SimpleFormatter());
            rootLogger.addHandler(fileHandler);

            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Could not set up logger: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if (clientMessage.equals("GET_QUOTE")) {
                        sendRandomQuote();
                    } else if (clientMessage.startsWith("CHAT:")) {
                        handleChat(clientMessage.substring(5));
                    } else if (clientMessage.startsWith("PROPOSE:")) {
                        handleProposedVideo(clientMessage.substring(8));
                    } else if (clientMessage.startsWith("VOTE:")) {
                        handleVote(clientMessage.substring(5));
                    }
                }
            } catch (IOException e) {
                logger.warning("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    synchronized (clients) {
                        clients.remove(this);
                    }
                } catch (IOException e) {
                    logger.severe("Error closing client socket: " + e.getMessage());
                }
            }
        }

        private void sendRandomQuote() {
            if (quotes.isEmpty()) {
                out.println("SERVER: No quotes available.");
                return;
            }

            Random random = new Random();
            String randomQuote = quotes.get(random.nextInt(quotes.size()));

            // Parse quote and author
            String[] parts = randomQuote.split(" - ", 2);
            if (parts.length == 2) {
                String quoteText = parts[0].trim();
                String author = parts[1].trim();

                out.println("QUOTE:" + quoteText + " - " + author);
                out.println("IMAGE:images/" + author.replace(" ", "_").toLowerCase() + ".jpg");
            } else {
                out.println("SERVER: Invalid quote format.");
            }
        }

        private void handleChat(String message) {
            broadcastMessage("CHAT:" + message);
        }

        private void handleProposedVideo(String videoUrl) {
            synchronized (proposedVideos) {
                if (!proposedVideos.contains(videoUrl)) {
                    proposedVideos.add(videoUrl);
                    broadcastProposals();
                } else {
                    out.println("SERVER: Video already proposed.");
                }
            }
        }

        private void handleVote(String videoUrl) {
            synchronized (videoVotes) {
                videoVotes.put(videoUrl, videoVotes.getOrDefault(videoUrl, 0) + 1);

                if (videoVotes.get(videoUrl) > clients.size() / 2) {
                    broadcastMessage("VIDEO_CHANGE:" + videoUrl);
                    proposedVideos.clear();
                    videoVotes.clear();
                } else {
                    broadcastProposals();
                }
            }
        }

        private void broadcastProposals() {
            StringBuilder proposalsMessage = new StringBuilder("PROPOSALS:");
            synchronized (proposedVideos) {
                for (String video : proposedVideos) {
                    proposalsMessage.append("\n").append(video).append(" (")
                            .append(videoVotes.getOrDefault(video, 0)).append(" votes)");
                }
            }
            broadcastMessage(proposalsMessage.toString());
        }

        private void broadcastMessage(String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.out.println(message);
                }
            }
        }
    }
}
