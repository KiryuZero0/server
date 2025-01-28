module com.example.demo3 {
    requires json.simple;
    requires javafx.fxml;
    requires javafx.web;
    requires okhttp3;
    requires com.google.gson;
    requires java.logging;


    opens com.example.demo3 to javafx.fxml;
    exports com.example.demo3;
}