package com.example.demo3;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.application.Platform;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


import java.io.*;
import java.net.Socket;
import java.util.*;

public class QuoteClient extends Application {
    // Fisiere
    private static final String VIDEOS_FILE = "src/main/resources/videos.txt";
    private ImageView authorImageView;

    // Chat Tab
    private ListView<String> chatListView = new ListView<>();
    private TextField chatInput = new TextField();
    private String username = "Guest";


    // Live Tab
    private ListView<String> videoListView = new ListView<>();
    private Map<String, List<String>> videoCategories = new HashMap<>();
    private ComboBox<String> categoryComboBox = new ComboBox<>();
    private TextField videoTitleInput = new TextField();
    private TextField videoUrlInput = new TextField();
    private Button addVideoButton = new Button("Add Video");
    private WebView videoWebView = new WebView();
    private CloudSyncService cloudSyncService = new CloudSyncService();
    // Server Connection
    private PrintWriter out;
   //setings
   private VBox root;
   //user
   private UserManager userManager = new UserManager();
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize categories
        videoCategories.putIfAbsent("Uncategorized", new ArrayList<>());
        categoryComboBox.getItems().addAll(videoCategories.keySet());
        categoryComboBox.setValue("Uncategorized");

        // Creare UI
        TabPane tabPane = new TabPane();
        Tab quotesTab = new Tab("Quotes", createQuotesTab());
        quotesTab.setClosable(false);
        Tab liveTab = new Tab("Live", createLiveTab());
        liveTab.setClosable(false);
        Tab chatTab = new Tab("Chat", createChatTab(username));
        chatTab.setClosable(false);
        Tab settingsTab = createSettingsTab();
        ;
        tabPane.getTabs().addAll(quotesTab, liveTab,  chatTab,settingsTab );
        tabPane.getTabs().add(createHistoryTab());

        root = new VBox(10, tabPane);
        root.setPadding(new Insets(10));
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(new Scene(createLoginScreen(userManager), 400, 300));
        primaryStage.show();

        connectToServer();
    }
    private VBox createMainApp(String username) {
        Label welcomeLabel = new Label("Welcome, " + username + "!");
        welcomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        TabPane tabPane = new TabPane();
        Tab quotesTab = new Tab("Quotes", createQuotesTab());
        quotesTab.setClosable(false);
        Tab liveTab = new Tab("Live", createLiveTab());
        liveTab.setClosable(false);
        Tab chatTab = new Tab("Chat", createChatTab(username));
        chatTab.setClosable(false);
        Tab settingsTab = createSettingsTab();
        ;
        tabPane.getTabs().addAll(quotesTab, liveTab,  chatTab,settingsTab );
        tabPane.getTabs().add(createHistoryTab());

        VBox mainAppLayout = new VBox(10, welcomeLabel, tabPane);
        mainAppLayout.setPadding(new Insets(10));
        return mainAppLayout;
    }

    private VBox createLoginScreen(UserManager userManager) {
        Label titleLabel = new Label("Login or Register");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        Label usernameLabel = new Label("Username:");
        TextField usernameInput = new TextField();

        Label passwordLabel = new Label("Password:");
        PasswordField passwordInput = new PasswordField();

        Button loginButton = new Button("Login");
        loginButton.setOnAction(event -> {
            String inputUsername = usernameInput.getText();
            String password = passwordInput.getText();
            if (userManager.authenticateUser(inputUsername, password)) {
                username = inputUsername;

                
                // Schimbă scena pentru a afișa aplicația principală
                VBox mainAppLayout = createMainApp(username); // Creează interfața principală
                Scene mainScene = new Scene(mainAppLayout, 800, 600);

                Stage primaryStage = (Stage) loginButton.getScene().getWindow(); // Obține scena curentă
                primaryStage.setScene(mainScene);
                primaryStage.setScene(new Scene(createMainApp(username), 800, 600)); // Trecem la aplicația principală


            } else {
                showError("Invalid username or password.");
            }
        });


        Button registerButton = new Button("Register");
        registerButton.setOnAction(event -> {
            String username = usernameInput.getText();
            String password = passwordInput.getText();
            if (userManager.registerUser(username, password)) {
                showInfo("Registration successful! Please log in.");
            } else {
                showError("Username already exists.");
            }
        });

        VBox loginBox = new VBox(10, titleLabel, usernameLabel, usernameInput, passwordLabel, passwordInput, loginButton, registerButton);
        loginBox.setPadding(new Insets(20));
        loginBox.setAlignment(Pos.CENTER);
        return loginBox;
    }
    private void setLightTheme() {
        root.setStyle("-fx-background-color: #ffffff;");
        showInfo("Light theme applied.");
    }

    private void setDarkTheme() {
        root.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: #ffffff;");
        showInfo("Dark theme applied.");
    }



    private VBox createQuotesTab() {
        Label quoteLabel = new Label("Press 'Next Quote' to fetch a random quote.");
        quoteLabel.setWrapText(true);
        quoteLabel.setFont(javafx.scene.text.Font.font("Verdana", FontWeight.BOLD, 20));
        quoteLabel.setTextFill(Color.DARKBLUE);

        authorImageView = new ImageView();
        authorImageView.setFitWidth(300);
        authorImageView.setFitHeight(300);
        authorImageView.setPreserveRatio(true);

        TextField searchField = new TextField();
        searchField.setPromptText("Search quotes...");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            List<String> filteredQuotes = loadQuotes().stream()
                    .filter(quote -> quote.toLowerCase().contains(newValue.toLowerCase()))
                    .toList();
            displayFilteredQuotes(quoteLabel, filteredQuotes);
        });

        Button nextQuoteButton = new Button("Next Quote");
        nextQuoteButton.setFont(javafx.scene.text.Font.font("Arial", FontWeight.BOLD, 14));
        nextQuoteButton.setStyle("-fx-background-color: lightblue;");
        nextQuoteButton.setOnAction(event -> displayRandomQuote(quoteLabel));

        VBox quotesBox = new VBox(10, searchField, quoteLabel, authorImageView, nextQuoteButton);
        quotesBox.setPadding(new Insets(10));
        quotesBox.setAlignment(Pos.CENTER);
        quotesBox.setStyle("-fx-background-color: #f0f0f0;");
        return quotesBox;
    }

    private void displayFilteredQuotes(Label quoteLabel, List<String> filteredQuotes) {
        if (!filteredQuotes.isEmpty()) {
            String randomQuote = filteredQuotes.get(new Random().nextInt(filteredQuotes.size()));
            String[] parts = randomQuote.split(" - ", 2);
            if (parts.length == 2) {
                String quoteText = parts[0].trim();
                String author = parts[1].trim();
                quoteLabel.setText("\"" + quoteText + "\"\n- " + author);
                loadAuthorImage(author);
            }
        } else {
            quoteLabel.setText("No matching quotes found.");
        }
    }


    private void displayRandomQuote(Label quoteLabel) {
        List<String> quotes = loadQuotes();
        if (quotes.isEmpty()) {
            showError("No quotes available.");
            return;
        }

        String randomQuote = quotes.get(new Random().nextInt(quotes.size()));
        String[] parts = randomQuote.split(" - ", 2);
        if (parts.length == 2) {
            String quoteText = parts[0].trim();
            String author = parts[1].trim();
            quoteLabel.setText("\"" + quoteText + "\"\n- " + author);
            loadAuthorImage(author);
        } else {
            showError("Invalid quote format.");
        }
    }


    private void loadAuthorImage(String author) {
        String imageName = author.replace(" ", "_").toLowerCase() + ".jpg";
        try {
            File imageFile = new File("src/main/resources/images/" + imageName);
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                authorImageView.setImage(image);
            } else {
                // Imagine implicită
                Image defaultImage = new Image(getClass().getResourceAsStream("src/main/resources/images/default_author.jpg"));
                authorImageView.setImage(defaultImage);
                showError("Image for author not found: " + author);
            }
        } catch (Exception e) {
            authorImageView.setImage(null);
            showError("Error loading image for author: " + author);
        }
    }



    private VBox createChatTab(String username) {
        chatListView.setPlaceholder(new Label("No messages yet."));
        chatListView.setStyle("-fx-background-color: #f9f9f9;");

        chatListView.setCellFactory(listView -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (item.startsWith(username + ":")) {
                            setStyle("-fx-background-color: #e0f7fa; -fx-font-weight: bold;");
                        } else {
                            setStyle(""); // Stil default
                        }
                    }
                }
            };

            ContextMenu contextMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("Edit");
            editItem.setOnAction(event -> {
                String selectedMessage = cell.getItem(); // Utilizează `cell` în contextul său
                if (selectedMessage != null) {
                    TextInputDialog dialog = new TextInputDialog(selectedMessage.split(": ", 2)[1]);
                    dialog.setTitle("Edit Message");
                    dialog.setHeaderText("Edit your message:");
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(newMessage -> {
                        String editedMessage = username + ": " + newMessage;
                        chatListView.getItems().set(chatListView.getItems().indexOf(selectedMessage), editedMessage);
                    });
                }
            });
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(event -> {
                String selectedMessage = cell.getItem(); // Utilizează `cell` în contextul său
                if (selectedMessage != null) {
                    Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmationDialog.setTitle("Delete Confirmation");
                    confirmationDialog.setHeaderText("Are you sure you want to delete this message?");
                    confirmationDialog.setContentText(selectedMessage);

                    Optional<ButtonType> result = confirmationDialog.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        chatListView.getItems().remove(selectedMessage);
                    }
                }
            });

            MenuItem copyItem = new MenuItem("Copy");
            copyItem.setOnAction(event -> {
                String selectedMessage = cell.getItem();
                if (selectedMessage != null) {
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                    content.putString(selectedMessage);
                    clipboard.setContent(content);
                    showInfo("Message copied to clipboard.");
                }
            });

            contextMenu.getItems().addAll(editItem, deleteItem,copyItem);
            cell.setContextMenu(contextMenu);
            return cell;
        });

        chatInput.setPromptText("Enter your message...");
        chatInput.setOnAction(event -> {
            String message = chatInput.getText().trim();
            if (!message.isEmpty()) {
                String fullMessage = username + ": " + message;
                chatListView.getItems().add(fullMessage);
                sendRequest("CHAT:" + fullMessage);
                chatInput.clear();
            }
        });

        VBox chatBox = new VBox(10, new Label("Chat:"), chatListView, chatInput);
        chatBox.setPadding(new Insets(10));
        chatBox.setStyle("-fx-background-color: #f9f9f9;");
        return chatBox;
    }
    private VBox createSettingsTabContent() {



        Label fontSettingsLabel = new Label("Font Settings:");
        fontSettingsLabel.setFont(javafx.scene.text.Font.font("Arial", FontWeight.BOLD, 16));

        ComboBox<String> fontFamilyComboBox = new ComboBox<>();
        fontFamilyComboBox.getItems().addAll("Arial", "Verdana", "Times New Roman", "Courier New", "Tahoma");
        fontFamilyComboBox.setValue("Arial");

        ComboBox<Integer> fontSizeComboBox = new ComboBox<>();
        fontSizeComboBox.getItems().addAll(12, 14, 16, 18, 20, 24, 28, 32);
        fontSizeComboBox.setValue(14);

        ComboBox<String> fontStyleComboBox = new ComboBox<>();
        fontStyleComboBox.getItems().addAll("Normal", "Bold", "Italic");
        fontStyleComboBox.setValue("Normal");

        Button applyFontSettingsButton = new Button("Apply Font Settings");
        applyFontSettingsButton.setOnAction(fontEvent -> {
            String selectedFontFamily = fontFamilyComboBox.getValue();
            int selectedFontSize = fontSizeComboBox.getValue();
            String selectedFontStyle = fontStyleComboBox.getValue();
            root.setStyle(String.format(
                    "-fx-font-family: '%s'; -fx-font-size: %dpx;",
                    selectedFontFamily,
                    selectedFontSize
            ));
        });

        ComboBox<String> themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll("Light", "Dark");
        themeComboBox.setValue("Light");

        themeComboBox.setOnAction(themeEvent -> {
            String selectedTheme = themeComboBox.getValue();
            if (selectedTheme.equals("Light")) {
                root.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000;");
            } else {
                root.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: #ffffff;");
            }
        });

        VBox settingsBox = new VBox(10, fontSettingsLabel,
                fontFamilyComboBox, fontSizeComboBox, fontStyleComboBox,
                applyFontSettingsButton, themeComboBox);
        settingsBox.setPadding(new Insets(10));
        settingsBox.setAlignment(Pos.CENTER);
        return settingsBox; // Returnăm VBox-ul
    }
    private Tab createSettingsTab() {
        Tab settingsTab = new Tab("Settings");
        settingsTab.setContent(createSettingsTabContent()); // Setăm conținutul tab-ului ca fiind un VBox
        settingsTab.setClosable(false);
        return settingsTab;
    }

    private VBox createLiveTab() {

        loadVideos();

        setupVideoContextMenu(); // Configurează meniul contextual

        // Configurare ComboBox pentru categorii
        Label categoryLabel = new Label("Category:");
        categoryComboBox.getItems().addAll(videoCategories.keySet());
        categoryComboBox.setValue("Uncategorized");
        categoryComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateVideoList(newValue);
        });
        Button fullscreenButton = new Button("Fullscreen");
        fullscreenButton.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white;");
        fullscreenButton.setOnAction(event -> {
            String currentUrl = videoWebView.getEngine().getLocation();
            if (currentUrl != null && !currentUrl.isEmpty()) {
                playVideoInFullscreen(currentUrl);
            } else {
                showError("No video is currently playing.");
            }
        });


        Button manageCategoriesButton = new Button("Manage Categories");
        manageCategoriesButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");

        HBox categoryBox = new HBox(10, categoryLabel, categoryComboBox, manageCategoriesButton);
        categoryBox.setAlignment(Pos.CENTER_LEFT);

        // Configurare ListView pentru videoclipuri
        Label videoListLabel = new Label("Videos:");
        videoListView.setPrefHeight(300);
        videoListView.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5px;");

        VBox videoListSection = new VBox(10, videoListLabel, videoListView);
        videoListSection.setPadding(new Insets(10));
        videoListSection.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-radius: 5px;");



        // Configurare player video (mărit)
        Label videoPlayerLabel = new Label("Video Player:");
        videoWebView.setPrefSize(800, 600); // Dimensiuni mari
        videoWebView.setStyle("-fx-border-color: #000; -fx-border-width: 2px; -fx-border-radius: 10px;");
        VBox videoPlayerSection = new VBox(10, videoPlayerLabel, videoWebView);
        videoPlayerSection.setPadding(new Insets(10));
        videoPlayerSection.setAlignment(Pos.CENTER);
        videoPlayerSection.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd; -fx-border-radius: 5px;");

        // Adăugare videoclipuri
        videoTitleInput.setPromptText("Enter video title...");
        videoUrlInput.setPromptText("Enter video URL...");
        addVideoButton.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white;");
        HBox addVideoBox = new HBox(10, videoTitleInput, videoUrlInput, addVideoButton);
        addVideoBox.setAlignment(Pos.CENTER_LEFT);

        VBox liveBox = new VBox(15,
                categoryBox,
                videoListSection,
                videoPlayerSection,
                addVideoBox
        );
        liveBox.setPadding(new Insets(20));
        liveBox.setStyle("-fx-background-color: #eef2f7;");
        return liveBox;
    }

    private void playVideoInFullscreen(String videoUrl) {
        Stage fullscreenStage = new Stage();
        fullscreenStage.initStyle(StageStyle.UNDECORATED); // Eliminăm bordura ferestrei
        fullscreenStage.setFullScreen(true); // Activăm modul fullscreen

        WebView fullscreenWebView = new WebView();
        fullscreenWebView.getEngine().load(videoUrl);
        fullscreenWebView.setPrefSize(Screen.getPrimary().getBounds().getWidth(),
                Screen.getPrimary().getBounds().getHeight());

        StackPane fullscreenPane = new StackPane(fullscreenWebView);
        fullscreenPane.setStyle("-fx-background-color: black;");

        Scene fullscreenScene = new Scene(fullscreenPane);
        fullscreenStage.setScene(fullscreenScene);

        fullscreenScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                fullscreenStage.close();
            }
        });

        fullscreenStage.show();
    }


    private void openCategoryManagementMenu(Button button) {
        ContextMenu categoryMenu = new ContextMenu();

        MenuItem addCategory = new MenuItem("Add Category");
        addCategory.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add Category");
            dialog.setHeaderText("Enter the name of the new category:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(categoryName -> {
                if (!videoCategories.containsKey(categoryName)) {
                    videoCategories.put(categoryName, new ArrayList<>());
                    categoryComboBox.getItems().add(categoryName);
                    showInfo("Category added successfully!");
                } else {
                    showError("Category already exists.");
                }
            });
        });


        MenuItem renameCategory = new MenuItem("Rename Category");
        renameCategory.setOnAction(event -> {
            String currentCategory = categoryComboBox.getValue();
            TextInputDialog dialog = new TextInputDialog(currentCategory);
            dialog.setTitle("Rename Category");
            dialog.setHeaderText("Enter the new name for the category:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newCategoryName -> {
                if (!videoCategories.containsKey(newCategoryName)) {
                    List<String> videos = videoCategories.remove(currentCategory);
                    videoCategories.put(newCategoryName, videos);
                    categoryComboBox.getItems().remove(currentCategory);
                    categoryComboBox.getItems().add(newCategoryName);
                    categoryComboBox.setValue(newCategoryName);
                    showInfo("Category renamed successfully!");
                } else {
                    showError("Category with this name already exists.");
                }
            });
        });




        MenuItem deleteCategory = new MenuItem("Delete Category");
        deleteCategory.setOnAction(event -> {
            String currentCategory = categoryComboBox.getValue();
            if (!currentCategory.equals("Uncategorized")) {
                Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
                confirmationDialog.setTitle("Delete Category");
                confirmationDialog.setHeaderText("Are you sure you want to delete this category?");
                confirmationDialog.setContentText("All videos in this category will be lost.");
                Optional<ButtonType> result = confirmationDialog.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    videoCategories.remove(currentCategory);
                    categoryComboBox.getItems().remove(currentCategory);
                    categoryComboBox.setValue("Uncategorized");
                    updateVideoList("Uncategorized");
                    showInfo("Category deleted successfully!");
                }
            } else {
                showError("Cannot delete the default category.");
            }
        });

        categoryMenu.getItems().addAll(addCategory, renameCategory, deleteCategory);
        categoryMenu.show(button, Side.BOTTOM, 0, 0);
    }

    private void loadVideos() {
        videoCategories.clear(); // Resetăm categoriile existente
        videoCategories.putIfAbsent("Uncategorized", new ArrayList<>());

        try (BufferedReader reader = new BufferedReader(new FileReader(VIDEOS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" - ", 2);
                if (parts.length == 2) {
                    String category = parts[0].trim();
                    String video = parts[1].trim();
                    videoCategories.putIfAbsent(category, new ArrayList<>());
                    videoCategories.get(category).add(video);
                }
            }
        } catch (IOException e) {
            showError("Failed to load videos.");
        }

        updateVideoList(categoryComboBox.getValue());
    }




    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private List<String> loadQuotes() {
        List<String> quotes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/quotes.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                quotes.add(line.trim());
            }
        } catch (IOException e) {
            showError("Failed to load quotes.");
        }
        return quotes;
    }

// Example: Add new tabs or extend features here if needed.

    private Map<String, Integer> videoPopularity = new HashMap<>();

    private void addVideoToCategory(String title, String url, String category) {
        String videoEntry = title + ": " + url;
        videoCategories.putIfAbsent(category, new ArrayList<>());
        videoCategories.get(category).add(videoEntry);

        saveVideosToFile();
        cloudSyncService.uploadUserData(username, videoCategories, chatListView.getItems());
    }

    private void addMessage(String message) {
        chatListView.getItems().add(message);
        cloudSyncService.uploadUserData(username, videoCategories, chatListView.getItems());
    }


    private void connectToServer() {
        try {
            Socket socket = new Socket("127.0.0.1", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            showError("Failed to connect to the server.");
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait(); // sau .show()
        });
    }

    private void sendRequest(String message) {
        if (out != null) { // Verifică dacă conexiunea este activă
            out.println(message);
        } else {
            showError("Server connection is not established.");
        }
    }


    private void updateVideoList(String category) {
        videoListView.getItems().clear();
        List<String> videos = videoCategories.getOrDefault(category, new ArrayList<>());
        videoListView.getItems().addAll(videos);
    }
    private void setupVideoContextMenu() {
        videoListView.setCellFactory(listView -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };

            // Creează meniul contextual
            ContextMenu contextMenu = new ContextMenu();

            // Opțiunea Play
            MenuItem playVideoItem = new MenuItem("Play");
            playVideoItem.setOnAction(event -> {
                String selectedVideo = cell.getItem();
                if (selectedVideo != null && selectedVideo.contains(": ")) {
                    String videoUrl = selectedVideo.split(": ", 2)[1];
                    playVideo(videoUrl); // Apelează metoda pentru redare
                }
            });

            // Opțiunea Delete Video
            MenuItem deleteVideoItem = new MenuItem("Delete Video");
            deleteVideoItem.setOnAction(event -> {
                String selectedVideo = cell.getItem();
                if (selectedVideo != null) {
                    // Șterge videoclipul din lista UI
                    videoListView.getItems().remove(selectedVideo);

                    // Șterge videoclipul din categoria curentă
                    videoCategories.get(categoryComboBox.getValue()).remove(selectedVideo);

                    // Actualizează fișierul cu videoclipuri
                    saveVideosToFile();
                }
            });
            MenuItem viewDetailsItem = new MenuItem("View Details");
            viewDetailsItem.setOnAction(event -> {
                String selectedVideo = cell.getItem();
                if (selectedVideo != null) {
                    String[] parts = selectedVideo.split(": ", 2);
                    String title = parts[0];
                    String url = parts.length > 1 ? parts[1] : "N/A";

                    Alert detailsDialog = new Alert(Alert.AlertType.INFORMATION);
                    detailsDialog.setTitle("Video Details");
                    detailsDialog.setHeaderText("Details for: " + title);
                    detailsDialog.setContentText("Title: " + title + "\nURL: " + url + "\nCategory: " + categoryComboBox.getValue());
                    detailsDialog.showAndWait();
                }
            });
            MenuItem fullscreenVideoItem = new MenuItem("Fullscreen");
            fullscreenVideoItem.setOnAction(event -> {
                String selectedVideo = cell.getItem();
                if (selectedVideo != null && selectedVideo.contains(": ")) {
                    String videoUrl = selectedVideo.split(": ", 2)[1];
                    playVideoInFullscreen(videoUrl);
                }
            });


            // Adaugă opțiunile în meniu
            contextMenu.getItems().addAll(playVideoItem, deleteVideoItem, fullscreenVideoItem, viewDetailsItem);
            cell.setContextMenu(contextMenu);

            return cell;
        });
    }
    private List<String> videoHistory = new ArrayList<>();
    private ListView<String> historyListView = new ListView<>();

    private void playVideo(String videoUrl) {
        if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
            videoWebView.getEngine().load(videoUrl);

            // Salvează în istoric
            videoHistory.add(videoUrl);
            historyListView.getItems().add(videoUrl);
        } else {
            showError("Invalid video URL: " + videoUrl);
        }
    }

    // Creează un tab pentru istoricul videoclipurilor
    private Tab createHistoryTab() {
        Label historyLabel = new Label("Video Playback History:");
        historyListView.setPrefHeight(400);
        VBox historyBox = new VBox(10, historyLabel, historyListView);
        historyBox.setPadding(new Insets(10));
        historyBox.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-radius: 5px;");

        Tab historyTab = new Tab("History");
        historyTab.setContent(historyBox);
        historyTab.setClosable(false);
        return historyTab;
    }
    // Metodă pentru salvarea videoclipurilor în fișier
    private void saveVideosToFile() {
        try (FileWriter writer = new FileWriter(VIDEOS_FILE)) {
            for (String category : videoCategories.keySet()) {
                for (String video : videoCategories.get(category)) {
                    writer.write(category + " - " + video + System.lineSeparator());
                }
            }
        } catch (IOException e) {
            showError("Failed to save updated video list.");
        }
    }

}









