package ste.ai.toolify.log;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

public class LogViewer extends VBox {

    @FXML
    private WebView webView;

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
        webView.getEngine().loadContent("<html><body></body></html>");
    }

    public void clear() {
        webView.getEngine().loadContent("<html><body></body></html>");
    }

    public String getText() {
        return (String) webView.getEngine().executeScript("document.documentElement.outerHTML");
    }

    public void log(String msg) {
        System.out.println("CHECK!!!");
        // Sanitize the message to prevent XSS and ensure valid HTML
        String sanitizedMsg = escapeHtml(msg);
        
        final String currentContent = getText();
        System.out.println("current content: " + currentContent);
        // Insert new message before the closing </body> tag
        final String newContent = currentContent.replace("</body>", sanitizedMsg + "<br></body>");
        webView.getEngine().loadContent(newContent);
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}