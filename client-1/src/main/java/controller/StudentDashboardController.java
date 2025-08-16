package controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import util.FeedbackService;
import util.LocationService;
import util.NoticeService;
import util.UserProfile;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class StudentDashboardController {

    @FXML private TextArea studentProfileArea;
    @FXML private ComboBox<String> routeComboBox;
    @FXML private Label locationLabel;
    @FXML private Label statusLabel;
    @FXML private Label etaLabel;
    @FXML private ImageView routeMapView;
    @FXML private TextArea notificationsArea;
    @FXML private TextArea altTransportArea;
    @FXML private TextArea noticeBoardArea;
    @FXML private TextArea feedbackArea;

    @FXML private TableView<String[]> busTable;
    @FXML private TableColumn<String[], String> busNoCol;
    @FXML private TableColumn<String[], String> routeCol;
    @FXML private TableColumn<String[], String> locationCol;
    @FXML private TableColumn<String[], String> timeCol;

    private UserProfile userProfile;
    private ScheduledExecutorService scheduler;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isNoticeUpdated = false;

    @FXML
    public void initialize() {
        routeComboBox.getItems().addAll("UIU ↔ Kuril", "UIU → Sayednagar → Family Bazar → Natunbajar → UIU");
        routeComboBox.getSelectionModel().selectFirst();

        notificationsArea.setText("No new notifications.");
        altTransportArea.setText("No delays detected. No alternative transport needed.");
        noticeBoardArea.setText("Welcome to Route Ease!");

        busNoCol.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(d.getValue()[3]));
        routeCol.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(d.getValue()[1]));
        locationCol.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(d.getValue()[2]));
        timeCol.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(d.getValue()[0]));

        updateDashboardForRoute(routeComboBox.getValue());
        startPolling();
        connectToServer();
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
        studentProfileArea.setText("Full Name: " + userProfile.getFullName() + "\nUIU ID: " + userProfile.getUiuId() +
                "\nDepartment: " + userProfile.getDepartment() + "\nUsername: " + userProfile.getUsername());
    }

    @FXML
    public void handleRouteSelection() {
        updateDashboardForRoute(routeComboBox.getValue());
        refreshLocationOnce();
    }

    @FXML
    private void handleLogout() {
        shutdown();
        try {
            Stage stage = (Stage) routeComboBox.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            stage.setScene(new Scene(loader.load(), 400, 500));
            stage.setTitle("Route Ease - Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFeedback() {
        String msg = feedbackArea.getText().trim();
        if (msg.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please type some feedback first.").showAndWait();
            return;
        }
        try {
            FeedbackService.add(userProfile.getUsername(), userProfile.getUiuId(), userProfile.getFullName(), msg);
            feedbackArea.clear();
            new Alert(Alert.AlertType.INFORMATION, "Feedback sent — thanks!").showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Could not send feedback.").showAndWait();
        }
    }

    private void startPolling() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            refreshLocationOnce();
            if (!isNoticeUpdated) refreshNoticeOnce();
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    private void refreshLocationOnce() {
        try {
            List<String[]> allRows = LocationService.readAllLocations();
            String selectedRoute = routeComboBox.getValue();
            Map<String, String[]> latestByBus = new LinkedHashMap<>();

            for (int i = allRows.size() - 1; i >= 0; i--) {
                String[] row = allRows.get(i);
                if (row.length >= 4 && row[1].equals(selectedRoute)) {
                    String busNo = row[3];
                    if (!latestByBus.containsKey(busNo)) {
                        latestByBus.put(busNo, row);
                    }
                }
            }

            List<String[]> filteredRows = new ArrayList<>(latestByBus.values());
            Platform.runLater(() -> busTable.setItems(FXCollections.observableArrayList(filteredRows)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshNoticeOnce() {
        try {
            String line = NoticeService.getLatest();
            if (line == null || !line.contains(",")) return;
            String[] p = line.split(",", 2);
            Platform.runLater(() -> {
                noticeBoardArea.setText(p[0] + "\n" + p[1]);
                isNoticeUpdated = true;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void updateDashboardForRoute(String route) {
        try {
            String path = route.startsWith("UIU ↔ Kuril") ? "/assets/kuril_route_map.png" : "/assets/natunbajar_route_map.png";
            var in = getClass().getResourceAsStream(path);
            routeMapView.setImage(in != null ? new Image(in) : null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(this::listenForUpdates).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForUpdates() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("LOCATION:")) {
                    String[] parts = message.substring("LOCATION:".length()).split(",");
                    if (parts.length == 3) {
                        String route = parts[0].trim();
                        String location = parts[1].trim();
                        String busNo = parts[2].trim();
                        String timestamp = new java.util.Date().toString();

                        Platform.runLater(() -> {
                            locationLabel.setText("Live: " + location + " (" + busNo + ")");
                            if (!route.equals(routeComboBox.getValue())) return;
                            busTable.getItems().removeIf(row -> row[3].equals(busNo));
                            busTable.getItems().add(new String[]{timestamp, route, location, busNo});
                            busTable.refresh();

                            try (BufferedWriter writer = new BufferedWriter(new FileWriter("data/locations.txt", true))) {
                                writer.write(timestamp + "," + route + "," + location + "," + busNo);
                                writer.newLine();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } else if (message.startsWith("NOTICE:")) {
                    String finalMessage = message;
                    Platform.runLater(() -> {
                        noticeBoardArea.setText(finalMessage);
                        isNoticeUpdated = true;
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
