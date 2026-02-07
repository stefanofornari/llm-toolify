package ste.ai.toolify;

import ste.ai.toolify.net.TrustAllCertificates;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        TrustAllCertificates.disableCertificateValidation();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ste/ai/toolify/main.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();

        primaryStage.setTitle("AI Toolify");
        primaryStage.setScene(new Scene(root, 800, 600));

        // Set application icon
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("app_icon.png")));

        // Save system prompt on application close
        primaryStage.setOnCloseRequest(event -> {
            controller.saveSystemPrompt();
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
