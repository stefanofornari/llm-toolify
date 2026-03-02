package ste.ai.toolify;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.BDDAssertions.then;
import ste.ai.toolify.tool.FileSystemTools;
import ste.ai.toolify.tool.UtilTools;
import static ste.lloop.Loop.on;

@ExtendWith(ApplicationExtension.class)
class MainControllerTest {

    private final String REQ = "HTTP request:\n- method: POST\n- url: https://api.perplexity.ai/chat/completions\n- headers: [content-type: application/json]\n- body: {\"msg\":\"this is the request #%s\"}";
    private final String RES = "HTTP response:\n- headers: [content-type: application/json]\n- body: {\"msg\":\"this is the response #%s\"}";

    private MainController controller;

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ste/ai/toolify/main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        stage.setScene(new javafx.scene.Scene(root));
        stage.setMaxWidth(800);
        stage.setMaxHeight(600);
        stage.show();
    }

    @Test
    public void llm_service_setup() {
        then(controller.llmService.tools).hasSize(2);
        then(controller.llmService.tools.get(0)).isInstanceOf(UtilTools.class);
        then(controller.llmService.tools.get(1)).isInstanceOf(FileSystemTools.class);
        then(controller.llmService.maxIterations()).isEqualTo(25);
    }

    @Test
    public void should_have_configuration_button(FxRobot robot) {
        robot.lookup("#configButton").queryAs(Button.class);
    }

    @Test
    public void send_chat_request_with_empty_user_prompt_logs_error_to_response_log_viewer(FxRobot robot) {
        // When

        robot.clickOn("#sendButton");

        // Then
        robot.interact(() -> {
            then((String)controller.llmResponseViewer().getEngine().executeScript("document.documentElement.outerHTML")).contains("User prompt must not be empty.");
        });
    }

    @Test
    public void log_viewers_show_their_own_records_only(FxRobot robot) {
        final Logger LOG = Logger.getLogger("dev.langchain4j.http.client.log");

        LOG.info(REQ);
        LOG.info(RES);

        robot.interact(() -> {
            Platform.runLater(()-> {
                then(controller.requestLogViewer().getText())
                    .contains("this is the request")
                    .doesNotContain("this is the response");
            });
            Platform.runLater(()-> {
                then(controller.responseLogViewer().getText())
                    .contains("this is the response")
                    .doesNotContain("this is the request");
            });

        });
    }

    @Test
    public void highligh_on_clicking_a_row(FxRobot robot) {
        final Logger LOG = Logger.getLogger("dev.langchain4j.http.client.log");

        for (String n: new String[] { "1", "2", "3" } ) {
            LOG.info(REQ.formatted(n));
            LOG.info(RES.formatted(n));
        }

        //
        // If request-1 is clicked, the corresponding response is highlighted
        //
        robot.interact(() -> {
            controller.onLogClick("request", "record-1");
        });

        robot.interact(() -> {
            on(true, false, false).loop((i, result) -> {
                Object o = controller.responseLogViewer().webView().getEngine().executeScript("""
                    recordDiv = document.getElementById("record-%d");
                    recordDiv ? recordDiv.classList.contains('highlighted') : false;
                """.formatted(i+1));
                then((boolean)o).isEqualTo(result);
            });
        });

        //
        // If response-3 is clicked, the corresponding request is highlighted
        //
        robot.interact(() -> {
            controller.onLogClick("response", "record-3");
        });

        robot.interact(() -> {
            on(false, false, true).loop((i, result) -> {
                Object o = controller.requestLogViewer().webView().getEngine().executeScript("""
                    recordDiv = document.getElementById("record-%d");
                    recordDiv ? recordDiv.classList.contains('highlighted') : false;
                """.formatted(i+1));
                then((boolean)o).isEqualTo(result);
            });
        });
    }

    @Test
    public void set_end_get_executor() {
        final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

        then(controller.executor()).isNotNull();

        controller.executor(EXECUTOR);
        then(controller.executor()).isEqualTo(EXECUTOR);
    }

    @Test
    public void run_llm_in_backgroun(FxRobot robot) throws Exception {
        final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
        final ExecutorCompletionService<Object> completionService
            = new ExecutorCompletionService<>(EXECUTOR);

        controller.executor(EXECUTOR);

            controller.userPromptTextArea().setText("this is a prompt");
        controller.sendChatRequest(); // trigger a new task to send the request

        completionService.poll(250, TimeUnit.MILLISECONDS);
    }

    @Test
    public void show_message_returned_by_llm(FxRobot robot) {
        List<JeddictBrainListener> listeners = controller.llmService.listeners();

        final UserMessage msg = UserMessage.from("Hello world!");
        final ChatRequest request = ChatRequest.builder().messages(UserMessage.from("Say hello")).build();
        final ChatResponse response = ChatResponse.builder().aiMessage(
            AiMessage.aiMessage("Hello World!")
        ).build();

        on(listeners).loop((listener) -> {
            listener.onResponse(request, response);
        });

        robot.interact(() -> {
            Object o = controller.llmResponseViewer().getEngine().executeScript("""
                document.getElementById('content').innerHTML
                """
            );
            then(String.valueOf(o)).contains("Hello World!");
        });
    }
}
