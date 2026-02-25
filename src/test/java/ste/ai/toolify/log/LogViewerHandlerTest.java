package ste.ai.toolify.log;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import javafx.application.Platform;

import static org.assertj.core.api.BDDAssertions.then;
import ste.ai.toolify.MainController;

@ExtendWith(ApplicationExtension.class)
class LogViewerHandlerTest {

    private final String LOG1 = "HTTP response:\n- status code: 200\n- headers: [content-type: application/json]\n- body: {\"id\":\"b19c37da-e5ae-44de-b463-d8d01ca7aacb\"}";
    private final String LOG2 = "HTTP response:\n- status code: 200\n- headers: [content-type: application/json]\n- body: {\"id\":\"2ffc1828-0474-11f1-b159-6f341d8c27e7\"}";

    private LogViewer logViewer;

    @Start
    private void start(Stage stage) {
        logViewer = new LogViewer();
        logViewer.controller(new MainController());
        stage.setScene(new Scene(logViewer, 800, 600));
        stage.show();
    }

    @Test
    void publish_logs_formatted_record_to_log_viewer(FxRobot robot) {
        // Given
        LogViewerHandler handler = new LogViewerHandler(null, logViewer);
        final LogRecord[] records = new LogRecord[] {
            new LogRecord(Level.INFO, LOG1),
            new LogRecord(Level.INFO, LOG2)
        };
        records[0].setLoggerName("test.logger");
        records[1].setLoggerName("test.logger");

        robot.interact(() -> {
            handler.publish(records[0]);
            Platform.runLater(() -> {
                then(logViewer.getText())
                    .contains("b19c37da-e5ae-44de-b463-d8d01ca7aacb");
            });

            handler.publish(records[1]);
            Platform.runLater(() -> {
                then(logViewer.getText())
                    .contains("b19c37da-e5ae-44de-b463-d8d01ca7aacb")
                    .contains("2ffc1828-0474-11f1-b159-6f341d8c27e7");
            });
        });
    }

    @Test
    void publish_with_matching_logger_name_logs_record(FxRobot robot) {
        // Given
        final LogViewerHandler handler = new LogViewerHandler("test.logger", logViewer);
        final LogRecord record = new LogRecord(Level.INFO, "- body: {\"msg\":\"Test message\"}");
        record.setLoggerName("test.logger");

        // When
        robot.interact(() -> {
            handler.publish(record);
        });

        robot.interact(() -> {
            then(logViewer.getText()).contains("Test message");
        });
    }

    @Test
    void publish_with_non_matching_logger_name_does_not_log_record(FxRobot robot) {
        // Given
        final LogViewerHandler handler = new LogViewerHandler("another.logger", logViewer);
        final LogRecord record = new LogRecord(Level.INFO, "{\"msg\":\"Test message\"}");
        record.setLoggerName("test.logger");

        // When
        robot.interact(() -> handler.publish(record));

        // Then
        robot.interact(() -> then(logViewer.getText()).doesNotContain("Test message"));
    }
}
