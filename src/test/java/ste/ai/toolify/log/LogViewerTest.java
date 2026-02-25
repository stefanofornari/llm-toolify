package ste.ai.toolify.log;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.BDDAssertions.then;
import ste.ai.toolify.MainController;

@ExtendWith(ApplicationExtension.class)
class LogViewerTest {

    private LogViewer logViewer;

    @Start
    private void start(Stage stage) {
        logViewer = new LogViewer();
        logViewer.controller(null, new MainController());
        stage.setScene(new Scene(logViewer, 800, 600));
        stage.show();
    }

    @Test
    void log_appends_json_object(FxRobot robot) {
        // Given
        final String json1 = "{\"type\": \"request\", \"headers\": {\"key1\": \"value1\"}, \"body\": \"long text 1\"}";
        final String json2 = "{\"type\": \"response\", \"headers\": {\"key2\": \"value2\"}, \"body\": \"long text 2\"}";

        robot.interact(() -> {
            logViewer.log(json1);
            logViewer.log(json2);

            Platform.runLater(() -> {
                then(logViewer.getText())
                    .contains("request").contains("id=\"record-1\"").contains("key1").contains("value1").contains("long text 1");
                then(logViewer.getText())
                   .contains("response").contains("id=\"record-2\"").contains("key2").contains("value2").contains("long text 2");
            });
        });
    }

    @Test
    void clear_removes_all_messages(FxRobot robot) {
        // Given
        final String json = "{\"type\": \"request\", \"headers\": {\"key\": \"value\"}, \"body\": \"long text\"}";
        robot.interact(() -> {
            logViewer.log(json);
        });
        Platform.runLater(() -> {
            then(logViewer.getText()).contains("long text");
        });

        robot.interact(() -> {
            logViewer.clear();
        });
        Platform.runLater(() -> {
            then(logViewer.getText()).doesNotContain("long text");
        });
    }
}