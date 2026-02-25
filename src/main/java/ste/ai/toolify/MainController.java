package ste.ai.toolify;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ste.ai.toolify.log.LogViewer;
import ste.ai.toolify.log.LogViewerHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import ste.ai.toolify.tool.FileSystemTools;


public class MainController {

    @FXML private TextArea systemPromptTextArea;
    @FXML private TextArea userPromptTextArea;
    @FXML private LogViewer requestLogViewer;
    @FXML private LogViewer responseLogViewer;
    @FXML private WebView llmResponseViewer;
    @FXML private Button sendButton;
    @FXML private Button configButton;
    @FXML private Button jsonButton;

    private HackerWithoutTools llmService; // Make it non-final so it can be re-initialized

    private Handler requestHandler;
    private Handler responseHandler;

    private ExecutorService executor = Executors.newCachedThreadPool();

    public MainController() {
    }

    private void initializeLlmService() {
        final Config config = new Config();

        try {
            this.llmService = new HackerWithoutTools(
                config.entry(Config.ENDPOINT),
                config.entry(Config.API_KEY),
                config.entry(Config.MODEL_NAME),
                (o) -> systemPromptTextArea.getText(),
                List.of(new FileSystemTools("."))
            );
        } catch (IOException x) {

        }
    }

    public ExecutorService executor() {
        return executor;
    }

    public void executor(final ExecutorService executor) {
        this.executor = executor;
    }

    @FXML
    private void initialize() {
        loadSystemPrompt();

        // Controllers setup
        requestLogViewer.controller("request", this);
        responseLogViewer.controller("response", this);

        // Initialize the logging handlers
        requestHandler = new LogViewerHandler("dev.langchain4j.http.client.log", requestLogViewer);
        responseHandler = new LogViewerHandler("dev.langchain4j.http.client.log", responseLogViewer);

        requestHandler.setFilter((record) -> record.getMessage().startsWith("HTTP request:"));
        responseHandler.setFilter((record) -> record.getMessage().startsWith("HTTP response:"));

        // Add the handlers to the respective loggers
        Logger.getLogger("dev.langchain4j.http.client.log").addHandler(requestHandler);
        Logger.getLogger("dev.langchain4j.http.client.log").addHandler(responseHandler);

        // Initiali llm response viewer
        final WebEngine engine = llmResponseViewer().getEngine();
        engine.setOnAlert((event) -> {
            System.out.println("ALERT: " + event.getData());
        });
        engine.setOnError((event) -> {
            System.out.println("ERROR: " + event.getMessage());
        });
        engine.load(getClass().getResource("llmviewer.html").toExternalForm());

        initializeLlmService();
    }

    @FXML
    protected void sendChatRequest() {
        String userPrompt = userPromptTextArea.getText();

        if(userPrompt.isEmpty()) {
            llmResponseViewer.getEngine().executeScript("content('User prompt must not be empty.');");
        }

        executor.execute(new Task<String>() {
            @Override
            protected String call() throws Exception {
                // Runs in background thread
                return llmService.chat(userPrompt);
            }

            @Override
            protected void succeeded() {
                JSObject window = (JSObject) llmResponseViewer.getEngine().executeScript("window");
                window.setMember("_llmText_", getValue());
                llmResponseViewer.getEngine().executeScript("content(window._llmText_);");
            }
        });
    }

    public void loadSystemPrompt() {
        final Config config = new Config();
        String savedPrompt = config.entry("system.prompt");
        if (savedPrompt != null && !savedPrompt.isEmpty()) {
            systemPromptTextArea.setText(savedPrompt);
        }
    }

    public void saveSystemPrompt() {
        final Config config = new Config();
        config.entry("system.prompt", systemPromptTextArea.getText());
    }

    @FXML
    private void showConfigDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ste/ai/toolify/config.fxml"));
            Parent root = loader.load();

            ConfigController configController = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Configuration");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(configButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));

            configController.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (configController.isSaveClicked()) {
                System.out.println("Configuration saved successfully");
                // Re-initialize LLMService with updated configuration
                initializeLlmService();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showJsonViewer() {
        try {
            Stage jsonStage = new Stage();
            jsonStage.setTitle("JSON Viewer");
            jsonStage.initModality(Modality.WINDOW_MODAL);
            jsonStage.initOwner(jsonButton.getScene().getWindow());

            WebView webView = new WebView();
            webView.getEngine().load(
                getClass().getResource("/ste/ai/toolify/json/jsonviewer.html").toExternalForm()
            );

            Scene scene = new Scene(webView, 800, 600);
            jsonStage.setScene(scene);
            jsonStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Clean up the handlers when the controller is no longer needed
    public void cleanup() {
        Logger.getLogger("dev.langchain4j.http.client.log").removeHandler(requestHandler);
        Logger.getLogger("dev.langchain4j.http.client.log").removeHandler(responseHandler);
    }

    public void onLogClick(final String source, final String recordId) {
        System.out.println("source: %s, id: %s".formatted(source, recordId));

        if ("request".equals(source)) {
            responseLogViewer.webView().getEngine().executeScript(
                "highlight(document.getElementById(\"%s\"));".formatted(recordId)
            );
        } else {
            requestLogViewer.webView().getEngine().executeScript(
                "highlight(document.getElementById(\"%s\"));".formatted(recordId)
            );
        }
    }

    public LogViewer requestLogViewer() {
        return requestLogViewer;
    }

    public LogViewer responseLogViewer() {
        return responseLogViewer;
    }

    public WebView llmResponseViewer() {
        return llmResponseViewer;
    }

    public TextArea userPromptTextArea() {
        return userPromptTextArea;
    }

}