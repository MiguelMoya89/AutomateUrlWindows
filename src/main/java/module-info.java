module com.example.proyectochatgpt {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.example.proyectochatgpt to javafx.fxml;
    exports com.example.proyectochatgpt;
}