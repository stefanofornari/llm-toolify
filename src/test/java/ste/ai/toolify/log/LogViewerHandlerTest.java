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

import static org.assertj.core.api.BDDAssertions.then;

@ExtendWith(ApplicationExtension.class)
class LogViewerHandlerTest {

    private LogViewer logViewer;

    @Start
    private void start(Stage stage) {
        logViewer = new LogViewer();
        stage.setScene(new Scene(logViewer, 800, 600));
        stage.show();
    }

    @Test
    void publish_logs_formatted_record_to_log_viewer(FxRobot robot) {
        // Given
        LogViewerHandler handler = new LogViewerHandler(null, logViewer);
        LogRecord record = new LogRecord(Level.INFO, "Test message");
        record.setLoggerName("test.logger");

        // When
        robot.interact(() -> handler.publish(record));

        // Then
        robot.interact(() -> {
            String text = logViewer.getText();
            then(text).contains("Test message");
            then(text).contains("test.logger");
        });
    }

    @Test
    void publish_with_matching_logger_name_logs_record(FxRobot robot) {
        // Given
        LogViewerHandler handler = new LogViewerHandler("test.logger", logViewer);
        LogRecord record = new LogRecord(Level.INFO, "Test message");
        record.setLoggerName("test.logger");

        // When
        robot.interact(() -> handler.publish(record));

        // Then
        robot.interact(() -> then(logViewer.getText()).contains("Test message"));
    }

    @Test
    void publish_with_non_matching_logger_name_does_not_log_record(FxRobot robot) {
        // Given
        LogViewerHandler handler = new LogViewerHandler("another.logger", logViewer);
        LogRecord record = new LogRecord(Level.INFO, "Test message");
        record.setLoggerName("test.logger");

        // When
        robot.interact(() -> handler.publish(record));

        // Then
        robot.interact(() -> then(logViewer.getText()).isEqualTo("<html><head></head><body></body></html>"));
    }
}
