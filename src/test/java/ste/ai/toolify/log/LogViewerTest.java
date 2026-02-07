package ste.ai.toolify.log;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.BDDAssertions.then;

@ExtendWith(ApplicationExtension.class)
class LogViewerTest {

    private LogViewer logViewer;

    @Start
    private void start(Stage stage) {
        logViewer = new LogViewer();
        stage.setScene(new Scene(logViewer, 800, 600));
        stage.show();
    }

    @Test
    void log_appends_message(FxRobot robot) {
        // Given
        String message1 = "First message";
        String message2 = "Second message";

        // When
        robot.interact(() -> logViewer.log(message1));
        robot.interact(() -> logViewer.log(message2));

        // Then
        robot.interact(() -> {
            then(logViewer.getText()).contains(message1);
            then(logViewer.getText()).contains(message2);
        });
    }

    @Test
    void clear_removes_all_messages(FxRobot robot) {
        // Given
        String message = "A message";
        robot.interact(() -> logViewer.log(message));
        robot.interact(() -> then(logViewer.getText()).contains(message));

        // When
        robot.interact(() -> logViewer.clear());

        // Then
        robot.interact(() -> then(logViewer.getText()).isEqualTo("<html><head></head><body></body></html>"));
    }
}
