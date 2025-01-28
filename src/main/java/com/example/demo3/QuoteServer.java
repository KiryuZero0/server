// QuoteServer.java - Server robust pentru citate
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

    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Integer> videoVotes = new ConcurrentHashMap<>();
    private static final List<String> proposedVideos = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> quotes = Collections.synchronizedList(new ArrayList<>());

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
                    clients.add(handler);
                    threadPool.submit(handler);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error accepting client connection: ", e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error starting server: ", e);
        } finally {
            threadPool.shutdown();
            logger.info("Server shut down.");
        }
    }

    // Încarcă citate din fișier
    private static void loadQuotes() {
        try (BufferedReader reader = new BufferedReader(new FileReader(QUOTES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                quotes.add(line.trim());
            }
            logger.info("Loaded " + quotes.size() + " quotes.");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load quotes: ", e);
        }
    }

    // Configurarea loggerului
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

    // Gestionarea fiecărui client
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
                    handleMessage(clientMessage);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error handling client: ", e);
            } finally {
                cleanup();
            }
        }

        // Gestionează mesajele primite de la client
        private void handleMessage(String clientMessage) {
            if (clientMessage.equals("GET_QUOTE")) {
                sendRandomQuote();
            } else if (clientMessage.startsWith("CHAT:")) {
                broadcastMessage(clientMessage.substring(5));
            } else if (clientMessage.startsWith("PROPOSE:")) {
                handleProposedVideo(clientMessage.substring(8));
            } else if (clientMessage.startsWith("VOTE:")) {
                handleVote(clientMessage.substring(5));
            }
        }

        // Trimite un citat aleatoriu clientului
        private void sendRandomQuote() {
            if (quotes.isEmpty()) {
                out.println("SERVER: No quotes available.");
                return;
            }
            Random random = new Random();
            String randomQuote = quotes.get(random.nextInt(quotes.size()));
            out.println("QUOTE:" + randomQuote);
        }

        // Trimite un mesaj către toți clienții
        private void broadcastMessage(String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.out.println("CHAT:" + message);
                }
            }
        }

        // Gestionează propunerea unui videoclip
        private void handleProposedVideo(String video) {
            synchronized (proposedVideos) {
                proposedVideos.add(video);
            }
            out.println("SERVER: Video proposed successfully.");
        }

        // Gestionează voturile pentru videoclipuri
        private void handleVote(String video) {
            videoVotes.merge(video, 1, Integer::sum);
            out.println("SERVER: Vote recorded for video: " + video);
        }

        // Cleanup la închiderea conexiunii
        private void cleanup() {
            try {
                clientSocket.close();
                synchronized (clients) {
                    clients.remove(this);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error closing client socket: ", e);
            }
        }
    }
}
