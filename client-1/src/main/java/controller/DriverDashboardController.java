package controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import util.AssignmentService;
import util.UserProfile;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DriverDashboardController {

    private UserProfile userProfile;

    @FXML private Label driverNameLabel;
    @FXML private Label driverIdLabel;
    @FXML private Label assignedBusLabel;
    @FXML private Label assignedRouteLabel;

    @FXML private ComboBox<String> busRouteComboBox;
    @FXML private ComboBox<String> locationComboBox;
    @FXML private Label statusLabel;

    private Socket socket;
    private PrintWriter out;

    @FXML
    public void initialize() {
        // Add routes
        busRouteComboBox.getItems().addAll(
                "UIU ↔ Kuril",
                "UIU → Sayednagar → Family Bazar → Natunbajar → UIU"
        );

        // Set listener for route change
        busRouteComboBox.setOnAction(e -> {
            String selectedRoute = busRouteComboBox.getValue();
            updateLocationOptions(selectedRoute);
        });

        // Set initial selection and corresponding locations
        busRouteComboBox.getSelectionModel().selectFirst();
        updateLocationOptions(busRouteComboBox.getValue());

        connectToServer();
    }

    private void updateLocationOptions(String selectedRoute) {
        locationComboBox.getItems().clear();

        if (selectedRoute.equals("UIU ↔ Kuril")) {
            locationComboBox.getItems().addAll(
                    "At UIU",
                    "300ft Expressway",
                    "Bashundhara R/A",
                    "On the way"
            );
        } else {
            locationComboBox.getItems().addAll(
                    "At UIU",
                    "Sayednagar",
                    "Family Bazar",
                    "Natunbajar",
                    "On the way"
            );
        }

        locationComboBox.getSelectionModel().selectFirst();
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
        updateDriverInfo();
        loadAssignment();
    }

    private void updateDriverInfo() {
        driverNameLabel.setText("Driver: " + userProfile.getFullName());
        driverIdLabel.setText("UIU ID: " + userProfile.getUiuId());
    }

    private void loadAssignment() {
        try {
            List<String[]> assignments = AssignmentService.loadAll();
            for (String[] record : assignments) {
                if (record[0].equals(userProfile.getUsername())) {
                    assignedBusLabel.setText("Assigned Bus: " + record[1]);
                    assignedRouteLabel.setText("Assigned Route: " + record[2]);
                    busRouteComboBox.getSelectionModel().select(record[2]);
                    updateLocationOptions(record[2]); // Ensure dropdown matches
                    return;
                }
            }
        } catch (IOException e) {
            assignedBusLabel.setText("Error loading assignment.");
        }
    }

    @FXML
    public void updateLocation() {
        String route = busRouteComboBox.getValue();
        String location = locationComboBox.getValue();
        String busNo = assignedBusLabel.getText().replace("Assigned Bus:", "").trim();

        statusLabel.setText("Bus " + busNo + " @ " + location);
        sendLocationUpdate(route, location, busNo);
    }

    private void sendLocationUpdate(String route, String location, String busNo) {
        if (out != null) {
            out.println("LOCATION: " + route + "," + location + "," + busNo);
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Stage stage = (Stage) busRouteComboBox.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            stage.setScene(new Scene(loader.load(), 400, 500));
            stage.setTitle("Route Ease - Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
