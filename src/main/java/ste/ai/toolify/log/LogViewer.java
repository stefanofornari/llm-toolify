package ste.ai.toolify.log;

import java.io.IOException;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import ste.ai.toolify.MainController;


public class LogViewer extends VBox {

    @FXML
    private WebView webView;

    @FXML
    protected VBox container;

    protected MainController controller;

    public LogViewer() {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("logviewer.fxml"));
        fxmlLoader.setRoot(this); // Set this instance as the root
        fxmlLoader.setController(this); // Set this instance as the controller
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    public void initialize() {
        final WebEngine engine = webView.getEngine();

        engine.setOnAlert((event) -> {
            System.out.println("ALERT: " + event.getData());
        });
        engine.setOnError((event) -> {
            System.out.println("ERROR: " + event.getMessage());
        });
        engine.getLoadWorker().stateProperty().addListener(
            (observable, oldState, state) -> {
                if (Worker.State.SUCCEEDED.equals(state)) {
                    Platform.runLater(() -> {
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("mainController", controller);
                    });
                }
            }
        );
    }

    public void clear() {
        webView.getEngine().executeScript("clear();");
    }

    public String getText() {
        return (String) webView.getEngine().executeScript("document.documentElement.outerHTML");
    }

    public void log(final String record) {
        Platform.runLater(() -> {
            webView.getEngine().executeScript("log(%s);".formatted(record));
        });
    }

    public WebView webView() {
        return webView;
    }

    public void controller(final String role, final MainController controller) {
        webView.getEngine().load(getClass().getResource("logviewer.html").toExternalForm() + "?role=" + role);
        this.controller = controller;
    }

    public void controller(final MainController controller) {
        controller("", controller);
    }
}