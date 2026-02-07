package ste.ai.toolify;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import ste.ai.toolify.log.LogViewer;

import static org.assertj.core.api.BDDAssertions.then;

@ExtendWith(ApplicationExtension.class)
class MainControllerTest {

    private MainController controller;

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ste/ai/toolify/main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        stage.setScene(new javafx.scene.Scene(root));
        stage.show();
    }

    @Test
    void should_have_configuration_button(FxRobot robot) {
        robot.lookup("#configButton").queryAs(Button.class);
    }

    @Test
    void send_chat_request_with_empty_user_prompt_logs_error_to_response_log_viewer(FxRobot robot) {
        // When
        robot.clickOn("#sendButton");

        // Then
        robot.interact(() -> {
            then(controller.getResponseLogViewer().getText()).contains("User prompt must not be empty.");
        });
    }
}
