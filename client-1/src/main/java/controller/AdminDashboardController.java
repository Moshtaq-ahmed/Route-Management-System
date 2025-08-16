package controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import util.AssignmentService;
import util.FeedbackService;
import util.FileHandler;
import util.NoticeService;

import java.io.*;
import java.net.*;
import java.util.List;

public class AdminDashboardController {

    /* ── FXML references ─────────────────────────────────────────────── */
    @FXML private TableView<String> driversTable;
    @FXML private TableColumn<String, String> drvCol;
    @FXML private TextField driverSearchField;

    @FXML private TableView<String[]> assignmentsTable;
    @FXML private TableColumn<String[], String> aDrvCol;
    @FXML private TableColumn<String[], String> aBusCol;
    @FXML private TableColumn<String[], String> aRouteCol;

    @FXML private TextField  busNumberField;
    @FXML private ComboBox<String> routeBox;
    @FXML private Label statusLabel;

    @FXML private TextArea noticeArea;

    /* feedback table */
    @FXML private TableView<String[]> feedbackTable;
    @FXML private TableColumn<String[], String> fTimeCol;
    @FXML private TableColumn<String[], String> fNameCol;
    @FXML private TableColumn<String[], String> fIdCol;
    @FXML private TableColumn<String[], String> fTextCol;

    /* ── data models ─────────────────────────────────────────────────── */
    private ObservableList<String>  masterDrivers;
    private FilteredList<String>    filteredDrivers;

    /* ── state ────────────────────────────────────────────────────────── */
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    /* ── INITIALISE ──────────────────────────────────────────────────── */
    @FXML
    public void initialize() {
        /* driver list & search */
        masterDrivers   = FXCollections.observableArrayList(loadDrivers());
        filteredDrivers = new FilteredList<>(masterDrivers, s -> true);
        driversTable.setItems(filteredDrivers);
        drvCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue()));
        driverSearchField.textProperty().addListener((obs, o, n) -> {
            String q = n.toLowerCase().trim();
            filteredDrivers.setPredicate(name -> q.isEmpty() || name.toLowerCase().contains(q));
        });

        /* route choices */
        routeBox.getItems().addAll(
                "UIU ↔ Kuril",
                "UIU → Sayednagar → Family Bazar → Natunbajar → UIU"
        );
        routeBox.getSelectionModel().selectFirst();

        /* assignments table */
        aDrvCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue()[0]));
        aBusCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue()[1]));
        aRouteCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue()[2]));
        assignmentsTable.getSelectionModel().selectedItemProperty().addListener((obs,o,n)->{
            if (n != null) {
                driversTable.getSelectionModel().select(n[0]);
                busNumberField.setText(n[1]);
                routeBox.getSelectionModel().select(n[2]);
            }
        });

        /* feedback table */
        fTimeCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue()[0]));
        fNameCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue()[3]));
        fIdCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue()[2]));
        fTextCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue()[4]));

        refreshAssignments();
        refreshFeedback();

        // Connect to the SocketServer
        connectToServer();
    }

    /* ── ASSIGNMENT BUTTONS ─────────────────────────────────────────── */
    @FXML
    public void handleAssign() {
        String driver = driversTable.getSelectionModel().getSelectedItem();
        String bus    = busNumberField.getText().trim();
        String route  = routeBox.getValue();

        if (driver == null || bus.isEmpty()) {
            statusLabel.setText("⚠ Select driver & enter bus.");
            return;
        }
        try {
            AssignmentService.save(driver, bus, route);
            statusLabel.setText("✅ Saved assignment for " + driver);
            refreshAssignments();
            busNumberField.clear();
        } catch (IOException e) {
            statusLabel.setText("❌ Could not save.");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRemove() {
        String driver = driversTable.getSelectionModel().getSelectedItem();
        if (driver == null) {
            statusLabel.setText("⚠ Select a driver first.");
            return;
        }
        try {
            AssignmentService.delete(driver);
            statusLabel.setText("🗑 Removed assignment for " + driver);
            refreshAssignments();
            busNumberField.clear();
        } catch (IOException e) {
            statusLabel.setText("❌ Could not remove.");
            e.printStackTrace();
        }
    }

    /* ── NOTICE BUTTON ──────────────────────────────────────────────── */
    @FXML
    public void handlePostNotice() {
        String msg = noticeArea.getText().trim();
        if (msg.isEmpty()) {
            statusLabel.setText("⚠ Notice text is empty.");
            return;
        }
        try {
            // 💾 Save to file permanently
            NoticeService.addNotice(msg);  // ⬅️ ADD THIS LINE

            // 🌐 Broadcast to all clients
            sendNoticeToServer(msg);

            statusLabel.setText("✅ Notice posted.");
            noticeArea.clear();
        } catch (IOException e) {
            statusLabel.setText("❌ Could not post notice.");
            e.printStackTrace();
        }
    }


    /* ── FEEDBACK BUTTON ────────────────────────────────────────────── */
    @FXML
    public void handleDeleteFeedback() {
        String[] selected = feedbackTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("⚠ Select a feedback row first.");
            return;
        }
        try {
            FeedbackService.delete(selected);
            statusLabel.setText("🗑 Deleted feedback from " + selected[3]);
            refreshFeedback();
        } catch (IOException e) {
            statusLabel.setText("❌ Could not delete feedback.");
            e.printStackTrace();
        }
    }

    /* ── LOGOUT ─────────────────────────────────────────────────────── */
    @FXML
    private void handleLogout() {
        try {
            Stage stage = (Stage) driversTable.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            stage.setScene(new Scene(loader.load(), 400, 500));
            stage.setTitle("Route Ease - Login");
            stage.setResizable(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ── helpers ─────────────────────────────────────────────────────── */
    private List<String> loadDrivers() {
        try {
            return FileHandler.loadAllUsersByRole("driver");
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private void refreshAssignments() {
        try {
            List<String[]> rows = AssignmentService.loadAll();
            assignmentsTable.setItems(FXCollections.observableArrayList(rows));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshFeedback() {
        try {
            List<String[]> rows = FeedbackService.loadAll();
            feedbackTable.setItems(FXCollections.observableArrayList(rows));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ── SOCKET SERVER INTEGRATION ────────────────────────────────────── */
    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);  // Connect to the server
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a new thread to listen for incoming notices
            new Thread(this::listenForNotices).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Listen for incoming notices and broadcast them to all connected clients
    private void listenForNotices() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                // Make the message final to be used inside the lambda expression
                final String finalMessage = message;
                Platform.runLater(() -> {
                    // Display the notice in real-time (optional)
                    statusLabel.setText("New Notice: " + finalMessage);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to send the notice to the server
    private void sendNoticeToServer(String notice) throws IOException {
        if (out != null) {
            out.println("NOTICE: " + notice);  // Send the notice to the server
        }
    }
}
