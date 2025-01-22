package com.example.demo3;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class QuoteServer {
    private static final int PORT = 12345;
    private static final int TIMEOUT_MS = 1 * 60 * 1000; // 5 minute în milisecunde
    private static final Map<String, String> characterImages = new HashMap<>();
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(QuoteServer.class.getName());

    public static void main(String[] args) {
        configureLogger(); // Configurare logger
        logger.info("Starting QuoteServer...");

        // Încarcă citate și imagini
        List<String> quotes = loadQuotes("quotes.txt");
        loadCharacterImages();

        if (quotes.isEmpty()) {
            logger.severe("No quotes found. Please check the file.");
            return;
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(10); // Pool cu maxim 10 fire

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setSoTimeout(TIMEOUT_MS); // Timeout de 5 minute
            logger.info("Server started on port " + PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Client connected: " + clientSocket.getInetAddress());

                    ClientHandler handler = new ClientHandler(clientSocket, quotes);
                    synchronized (clients) {
                        clients.add(handler);
                    }
                    threadPool.submit(handler);
                } catch (SocketTimeoutException e) {
                    logger.warning("No client connected for 5 minutes. Server shutting down...");
                    break; // Ieșim din bucla principală
                }
            }
        } catch (IOException e) {
            logger.severe("Error starting server: " + e.getMessage());
        } finally {
            threadPool.shutdown();
            logger.info("Server shut down.");
        }
    }

    private static List<String> loadQuotes(String filename) {
        List<String> quotes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                quotes.add(line);
            }
            logger.info("Loaded " + quotes.size() + " quotes from file.");
        } catch (IOException e) {
            logger.severe("Error reading quotes file: " + e.getMessage());
        }
        return quotes;
    }

    private static void loadCharacterImages() {
        characterImages.put("Goku", "C:\\Users\\Zero\\Documents\\GitHub\\PCD\\ACS1\\src\\main\\java\\images\\goku.jpg");
        characterImages.put("Naruto", "C:\\Users\\Zero\\Documents\\GitHub\\PCD\\ACS1\\src\\main\\java\\images\\naruto.jpg");
        characterImages.put("Luffy", "C:\\Users\\Zero\\Documents\\GitHub\\PCD\\ACS1\\src\\main\\java\\images\\luffy.jpg");
        characterImages.put("Mikasa", "C:\\Users\\Zero\\Documents\\GitHub\\PCD\\ACS1\\src\\main\\java\\images\\mikasa.jpg");
        characterImages.put("Edward", "C:\\Users\\Zero\\Documents\\GitHub\\PCD\\ACS1\\src\\main\\java\\images\\edward.jpg");
        characterImages.put("Default", "C:\\Users\\Zero\\Documents\\GitHub\\PCD\\ACS1\\src\\main\\java\\images\\default.jpg");
        logger.info("Loaded character image mappings.");
    }

    private static void configureLogger() {
        try {
            LogManager.getLogManager().reset();
            Logger rootLogger = Logger.getLogger("");
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(new SimpleFormatter());
            rootLogger.addHandler(consoleHandler);

            FileHandler fileHandler = new FileHandler("server.log", true); // Log în fișier
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SimpleFormatter());
            rootLogger.addHandler(fileHandler);

            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Could not set up logger: " + e.getMessage());
        }
    }

    // Client handler pentru gestionarea fiecărui client conectat
    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final List<String> quotes;
        private PrintWriter out;

        public ClientHandler(Socket clientSocket, List<String> quotes) {
            this.clientSocket = clientSocket;
            this.quotes = quotes;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                this.out = out;
                logger.info("Handling client: " + clientSocket.getInetAddress());

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if (clientMessage.equalsIgnoreCase("GET")) {
                        sendRandomQuote();
                    } else if (clientMessage.startsWith("CHAT:")) {
                        broadcastMessage(clientMessage.substring(5).trim());
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
                    logger.info("Client disconnected: " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    logger.severe("Error closing client socket: " + e.getMessage());
                }
            }
        }

        private void sendRandomQuote() {
            Random random = new Random();
            String quote = quotes.get(random.nextInt(quotes.size()));

            String character = extractCharacterName(quote);
            String imagePath = characterImages.getOrDefault(character, "images/default.jpg");

            out.println("QUOTE:" + quote);
            out.println("IMAGE:" + imagePath);

            logger.info("Sent quote: " + quote);
            logger.info("Sent image: " + imagePath);
        }

        private String extractCharacterName(String quote) {
            if (quote.contains("-")) {
                return quote.substring(quote.lastIndexOf('-') + 1).trim();
            }
            return "Default";
        }

        private void broadcastMessage(String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.out.println("CHAT:" + message);
                }
            }
            logger.info("Broadcasted message: " + message);
        }
    }
}
