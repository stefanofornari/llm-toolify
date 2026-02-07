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
import java.util.logging.Handler;
import java.util.logging.Logger;
// Removed static imports for Config constants

public class MainController {

    @FXML private javafx.scene.control.TextArea systemPromptTextArea;
    @FXML private javafx.scene.control.TextArea userPromptTextArea;
    @FXML private LogViewer requestLogViewer;
    @FXML private LogViewer responseLogViewer;
    @FXML private javafx.scene.control.TextArea llmResponseTextArea;
    @FXML private Button sendButton;
    @FXML private Button configButton;
    @FXML private Button jsonButton;

    public LogViewer getRequestLogViewer() {
        return requestLogViewer;
    }

    public LogViewer getResponseLogViewer() {
        return responseLogViewer;
    }

    private LLMService llmService; // Make it non-final so it can be re-initialized

    private Handler requestHandler;
    private Handler responseHandler;

    public MainController() {
        initializeLlmService();
    }

    private void initializeLlmService() {
        final Config config = new Config();
        this.llmService = new LLMService(
            config.entry(Config.ENDPOINT),
            config.entry(Config.API_KEY),
            config.entry(Config.MODEL_NAME),
            (something) -> systemPromptTextArea.getText()
        );
    }

    @FXML
    private void initialize() {
        loadSystemPrompt();

        // Initialize the logging handlers
        requestHandler = new LogViewerHandler("dev.langchain4j.http.client.log", requestLogViewer);
        responseHandler = new LogViewerHandler("dev.langchain4j.http.client.log", responseLogViewer);

        // Add the handlers to the respective loggers
        Logger.getLogger("dev.langchain4j.http.client.log").addHandler(requestHandler);
        Logger.getLogger("dev.langchain4j.http.client.log").addHandler(responseHandler);
    }

    @FXML
    private void sendChatRequest() {
        String userPrompt = userPromptTextArea.getText();

        if(userPrompt.isEmpty()) {
            responseLogViewer.log("User prompt must not be empty.");
            return;
        }

        // Clear the log viewers before sending the chat request
        requestLogViewer.clear();
        responseLogViewer.clear();

        // Call the LLM service to get the chat completion
        String llmResponse = llmService.chat(userPrompt);
        llmResponseTextArea.setText(llmResponse);
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
}