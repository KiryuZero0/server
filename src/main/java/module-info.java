module com.example.demo3 {
    requires json.simple;
    requires javafx.fxml;
    requires java.logging;
    requires javafx.web;


    opens com.example.demo3 to javafx.fxml;
    exports com.example.demo3;
}