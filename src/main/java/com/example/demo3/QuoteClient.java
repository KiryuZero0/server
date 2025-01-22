package com.example.demo3;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;

public class QuoteClient extends Application {
    private static final String SERVER_IP = "127.0.0.1"; // Server IP (localhost pentru server local)
    private static final int SERVER_PORT = 12345;        // Portul utilizat de server

    // UI Components
    private TextArea chatArea = new TextArea();
    private TextField chatInput = new TextField();
    private Label quoteLabel = new Label("Press 'Get Quote' to fetch a random anime quote.");
    private ImageView imageView = new ImageView();

    // Server connection
    private PrintWriter out;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Layout principal
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Zona de citate
        Button getQuoteButton = new Button("Get Quote");
        getQuoteButton.setOnAction(event -> sendRequest("GET"));

        imageView.setFitWidth(300); // Lățimea imaginii
        imageView.setFitHeight(300); // Înălțimea imaginii
        imageView.setPreserveRatio(true);

        VBox quoteBox = new VBox(10, quoteLabel, imageView, getQuoteButton);
        quoteBox.setPadding(new Insets(10));
        quoteBox.setStyle("-fx-border-color: gray; -fx-border-width: 1; -fx-alignment: center;");

        // Zona de chat
        chatArea.setEditable(false); // Chat-ul este doar pentru citire
        chatInput.setPromptText("Enter your message...");
        chatInput.setOnAction(event -> {
            String message = chatInput.getText().trim();
            if (!message.isEmpty()) {
                sendRequest("CHAT:" + message);
                chatInput.clear(); // Șterge inputul după trimitere
            }
        });

        ScrollPane chatScroll = new ScrollPane(chatArea);
        chatScroll.setFitToWidth(true);

        VBox chatBox = new VBox(10, new Label("Chat:"), chatScroll, chatInput);
        chatBox.setPadding(new Insets(10));
        chatBox.setStyle("-fx-border-color: gray; -fx-border-width: 1; -fx-alignment: center;");

        // Adăugare secțiuni în layout principal
        root.getChildren().addAll(quoteBox, chatBox);

        // Configurare scenă
        Scene scene = new Scene(root, 500, 700);
        primaryStage.setTitle("Anime Quote Client");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Conectare la server
        connectToServer();
    }

    private void connectToServer() {
        try {
            // Conectare la server
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Ascultare mesaje de la server într-un thread separat
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        final String message = serverMessage; // Variabilă finală pentru a fi utilizată în lambda
                        Platform.runLater(() -> {
                            if (message.startsWith("QUOTE:")) {
                                String quote = message.substring(6).trim();
                                quoteLabel.setText(quote);
                            } else if (message.startsWith("IMAGE:")) {
                                String imagePath = message.substring(6).trim();
                                imageView.setImage(new Image("file:" + imagePath));
                            } else if (message.startsWith("CHAT:")) {
                                chatArea.appendText(message.substring(5).trim() + "\n");
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to connect to the server. Please check the server status.");
        }
    }

    private void sendRequest(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}


