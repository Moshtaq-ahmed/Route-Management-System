package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point.  Loads login.fxml then lets controllers swap scenes.
 * The Stage is now resizable so the user can maximise the window.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader =
                new FXMLLoader(getClass().getResource("/view/login.fxml"));

        // Initial size only; user can resize later
        Scene scene = new Scene(loader.load(), 400, 500);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Route Ease - Login");
        primaryStage.setResizable(true);        // ← allow maximise / resize
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
