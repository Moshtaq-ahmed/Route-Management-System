package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import util.FileHandler;
import util.UserProfile;

import java.io.IOException;

public class LoginController {

    @FXML private TextField   usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleBox;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        roleBox.getItems().addAll("student", "driver", "admin");
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String role     = roleBox.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            errorLabel.setText("Please fill all fields.");
            return;
        }

        try {
            if (!FileHandler.validateUser(username, password, role)) {
                errorLabel.setText("Invalid credentials or role.");
                return;
            }

            UserProfile user = FileHandler.loadUserProfile(username);
            if (user == null) {
                errorLabel.setText("User profile not found.");
                return;
            }

            Stage stage  = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader;

            switch (role) {
                case "student" -> loader = new FXMLLoader(getClass().getResource("/view/student_dashboard.fxml"));
                case "driver"  -> loader = new FXMLLoader(getClass().getResource("/view/driver_dashboard.fxml"));
                case "admin"   -> loader = new FXMLLoader(getClass().getResource("/view/admin_dashboard.fxml"));
                default        -> {
                    errorLabel.setText("Unknown role.");
                    return;
                }
            }

            Scene scene = new Scene(loader.load(), 900, 600);
            stage.setScene(scene);
            stage.setTitle("Route Ease - " + Character.toUpperCase(role.charAt(0)) + role.substring(1));
            stage.setResizable(true);                       // ← keep window resizable

            /* Pass user profile to dashboards that need it */
            switch (role) {
                case "student" -> {
                    StudentDashboardController c = loader.getController();
                    c.setUserProfile(user);
                    stage.setOnCloseRequest(e -> c.shutdown());
                }
                case "driver"  -> {
                    DriverDashboardController c = loader.getController();
                    c.setUserProfile(user);
                    /* no long-running thread here */
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            errorLabel.setText("Error loading dashboard.");
        }
    }

    @FXML
    public void goToSignup() {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/signup.fxml"));
            stage.setScene(new Scene(loader.load(), 400, 500));
            stage.setTitle("Route Ease - Sign Up");
            stage.setResizable(true);          // signup window too
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
