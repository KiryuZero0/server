package com.example.demo3;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;

public class QuoteClient extends Application {
    private static final String VIDEOS_FILE = "src/main/resources/videos.txt";
    private static final String USERNAME_FILE = "src/main/resources/username.txt";
    private static final String QUOTES_FILE = "src/main/resources/quotes.txt";
    private static final String USERS_FILE = "src/main/resources/users.json";
    private static final String IMAGES_DIR = "/images/";

    // Quotes Tab Components
    private Label quoteLabel = new Label("Press 'Next Quote' to fetch a random quote.");
    private ImageView authorImageView = new ImageView();
    private Button nextQuoteButton = new Button("Next Quote");
    private List<String> quotes = new ArrayList<>();

    // Live Tab Components
    private ListView<String> videoListView = new ListView<>();
    private WebView videoWebView = new WebView();
    private TextField videoTitleInput = new TextField();
    private TextField videoUrlInput = new TextField();
    private Button addVideoButton = new Button("Add Video");

    // Chat Tab Components
    private TextArea chatArea = new TextArea();
    private TextField chatInput = new TextField();
    private String username = "Guest";

    // Server Connection
    private PrintWriter out;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Load or Register User
        checkOrRegisterUser();

        // Load quotes
        loadQuotes();

        // Create tabs
        TabPane tabPane = new TabPane();
        Tab quotesTab = new Tab("Quotes", createQuotesTab());
        quotesTab.setClosable(false);
        Tab liveTab = new Tab("Live", createLiveTab());
        liveTab.setClosable(false);
        Tab chatTab = new Tab("Chat", createChatTab());
        chatTab.setClosable(false);
        tabPane.getTabs().addAll(quotesTab, liveTab, chatTab);

        // Add "Change Username" button
        Button changeUsernameButton = new Button("Change Username");
        changeUsernameButton.setOnAction(event -> changeUsername());

        VBox root = new VBox(10, changeUsernameButton, tabPane);
        root.setPadding(new Insets(10));

        // Set up the scene
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Anime Quote Client - " + username);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Connect to the server
        connectToServer();
    }

    private VBox createQuotesTab() {
        // Configure author image view
        authorImageView.setFitWidth(300);
        authorImageView.setFitHeight(300);
        authorImageView.setPreserveRatio(true);

        // Configure "Next Quote" button
        nextQuoteButton.setOnAction(event -> displayRandomQuote());

        VBox quotesBox = new VBox(10, quoteLabel, authorImageView, nextQuoteButton);
        quotesBox.setPadding(new Insets(10));
        quotesBox.setStyle("-fx-alignment: center; -fx-background-color: #f0f0f0;");
        return quotesBox;
    }

    private VBox createLiveTab() {
        loadVideos();

        videoListView.setPlaceholder(new Label("No videos available."));
        videoListView.setOnMouseClicked(event -> {
            String selectedVideo = videoListView.getSelectionModel().getSelectedItem();
            if (selectedVideo != null && selectedVideo.contains(": ")) {
                String videoUrl = selectedVideo.split(": ", 2)[1];
                videoWebView.getEngine().load(videoUrl);
            }
        });

        // Configure Add Video Components
        videoTitleInput.setPromptText("Enter video title...");
        videoUrlInput.setPromptText("Enter video URL...");
        addVideoButton.setOnAction(event -> addVideo());

        HBox addVideoBox = new HBox(10, videoTitleInput, videoUrlInput, addVideoButton);
        addVideoBox.setAlignment(Pos.CENTER);

        VBox liveBox = new VBox(10,
                new Label("Available Videos:"),
                videoListView,
                new Label("Video Player:"),
                videoWebView,
                new Label("Add a New Video:"),
                addVideoBox
        );
        liveBox.setPadding(new Insets(10));
        liveBox.setStyle("-fx-background-color: #e6f7ff;");
        return liveBox;
    }

    private VBox createChatTab() {
        chatArea.setEditable(false);
        chatInput.setPromptText("Enter your message...");
        chatInput.setOnAction(event -> {
            String message = chatInput.getText().trim();
            if (!message.isEmpty()) {
                String fullMessage = username + ": " + message;
                sendRequest("CHAT:" + fullMessage);
                chatInput.clear();
            }
        });

        VBox chatBox = new VBox(10, new Label("Chat:"), new ScrollPane(chatArea), chatInput);
        chatBox.setPadding(new Insets(10));
        chatBox.setStyle("-fx-background-color: #f9f9f9;");
        return chatBox;
    }

    private void checkOrRegisterUser() {
        File usersFile = new File(USERS_FILE);
        if (!usersFile.exists()) {
            try {
                usersFile.createNewFile();
                JSONArray usersArray = new JSONArray();
                try (FileWriter writer = new FileWriter(usersFile)) {
                    writer.write(usersArray.toJSONString());
                }
            } catch (IOException e) {
                showError("Failed to create users file.");
            }
        }

        try {
            JSONParser parser = new JSONParser();
            JSONArray usersArray = (JSONArray) parser.parse(new FileReader(USERS_FILE));

            TextInputDialog usernameDialog = new TextInputDialog();
            usernameDialog.setTitle("User Registration");
            usernameDialog.setHeaderText("Enter your username:");
            usernameDialog.setContentText("Username:");
            Optional<String> usernameInput = usernameDialog.showAndWait();

            if (usernameInput.isPresent()) {
                username = usernameInput.get().trim();

                boolean userExists = usersArray.stream()
                        .anyMatch(user -> username.equals(((JSONObject) user).get("username")));

                if (!userExists) {
                    TextInputDialog passwordDialog = new TextInputDialog();
                    passwordDialog.setTitle("User Registration");
                    passwordDialog.setHeaderText("Set your password:");
                    passwordDialog.setContentText("Password:");
                    Optional<String> passwordInput = passwordDialog.showAndWait();

                    if (passwordInput.isPresent()) {
                        String password = passwordInput.get().trim();
                        JSONObject newUser = new JSONObject();
                        newUser.put("username", username);
                        newUser.put("password", password);
                        usersArray.add(newUser);

                        try (FileWriter writer = new FileWriter(usersFile)) {
                            writer.write(usersArray.toJSONString());
                        }

                        showInfo("User registered successfully!");
                    } else {
                        showError("Registration failed: No password provided.");
                        System.exit(0);
                    }
                } else {
                    showInfo("Welcome back, " + username + "!");
                }
            } else {
                showError("Registration failed: No username provided.");
                System.exit(0);
            }
        } catch (Exception e) {
            showError("Failed to load users.");
        }
    }

    private void loadQuotes() {
        try (BufferedReader reader = new BufferedReader(new FileReader(QUOTES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                quotes.add(line.trim());
            }
        } catch (IOException e) {
            showError("Failed to load quotes from file.");
        }
    }

    private void loadVideos() {
        try (BufferedReader reader = new BufferedReader(new FileReader(VIDEOS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                videoListView.getItems().add(line.trim());
            }
        } catch (IOException e) {
            showError("Failed to load videos.");
        }
    }

    private void addVideo() {
        String title = videoTitleInput.getText().trim();
        String url = videoUrlInput.getText().trim();
        if (title.isEmpty() || url.isEmpty()) {
            showError("Both title and URL must be provided.");
            return;
        }

        String videoEntry = title + ": " + url;
        videoListView.getItems().add(videoEntry);

        try (FileWriter writer = new FileWriter(VIDEOS_FILE, true)) {
            writer.write(videoEntry + System.lineSeparator());
            showInfo("Video added successfully!");
        } catch (IOException e) {
            showError("Failed to save video.");
        }

        videoTitleInput.clear();
        videoUrlInput.clear();
    }

    private void displayRandomQuote() {
        if (quotes.isEmpty()) {
            showError("No quotes available.");
            return;
        }

        String randomQuote = quotes.get(new Random().nextInt(quotes.size()));
        String[] parts = randomQuote.split(" - ", 2);
        if (parts.length == 2) {
            String quoteText = parts[0].trim();
            String author = parts[1].trim();
            quoteLabel.setText("\"" + quoteText + "\" - " + author);
            loadAuthorImage(author);
        } else {
            showError("Invalid quote format: " + randomQuote);
        }
    }

    private void loadAuthorImage(String author) {
        String imageName = author.replace(" ", "_").toLowerCase() + ".jpg";
        try {
            Image image = new Image(getClass().getResourceAsStream(IMAGES_DIR + imageName));
            authorImageView.setImage(image);
        } catch (Exception e) {
            authorImageView.setImage(null);
        }
    }

    private void changeUsername() {
        TextInputDialog dialog = new TextInputDialog(username);
        dialog.setTitle("Change Username");
        dialog.setHeaderText("Enter your new username:");
        dialog.setContentText("Username:");
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(newUsername -> {
            username = newUsername.trim();
            showInfo("Username updated to: " + username);
        });
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("127.0.0.1", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        final String message = serverMessage;
                        Platform.runLater(() -> {
                            if (message.startsWith("CHAT:")) {
                                chatArea.appendText(message.substring(5).trim() + "\n");
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            showError("Failed to connect to the server.");
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
            alert.show();
        });
    }

    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }
}
