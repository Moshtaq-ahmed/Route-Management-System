package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import util.FileHandler;

import java.io.IOException;

public class SignupController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleBox;

    @FXML private TextField fullNameField;
    @FXML private TextField uiuIdField;
    @FXML private TextField departmentField;

    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        roleBox.getItems().addAll("student", "driver", "admin");
    }

    @FXML
    public void handleSignup() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String role = roleBox.getValue();
        String fullName = fullNameField.getText().trim();
        String uiuId = uiuIdField.getText().trim();
        String department = departmentField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || role == null ||
                fullName.isEmpty() || uiuId.isEmpty() || department.isEmpty()) {
            messageLabel.setText("Please fill all fields.");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            if (FileHandler.addUser(username, password, role, fullName, uiuId, department)) {
                System.out.println("User registered: " + username);
                messageLabel.setText("Registration successful! Go to login.");
                messageLabel.setStyle("-fx-text-fill: green;");
                messageLabel.setVisible(true);
            } else {
                messageLabel.setText("Username already exists.");
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setVisible(true);
            }
        } catch (IOException e) {
            messageLabel.setText("Error occurred.");
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setVisible(true);
            e.printStackTrace();
        }
    }

    @FXML
    public void goToLogin() throws IOException {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
        stage.setScene(new Scene(loader.load(), 400, 500));
        stage.setTitle("Route Ease - Login");
    }
}
