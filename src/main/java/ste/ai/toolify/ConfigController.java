package ste.ai.toolify;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
// Removed dev.dirs.ProjectDirectories import as it's now handled by Config

public class ConfigController {

    @FXML private TextField endpointField;
    @FXML private TextField keyField;
    @FXML private TextField modelField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Hyperlink settingsFolderLink;

    private Stage dialogStage;
    private boolean saveClicked = false;
    private final Config config; 

    public ConfigController() {
        this.config = new Config(); 
    }

    @FXML
    private void initialize() {
        endpointField.setText(config.entry(Config.ENDPOINT));
        keyField.setText(config.entry(Config.API_KEY));
        modelField.setText(config.entry(Config.MODEL_NAME));
    }

    @FXML
    private void handleSave() {
        config.entry(Config.ENDPOINT, endpointField.getText());
        config.entry(Config.API_KEY, keyField.getText());
        config.entry(Config.MODEL_NAME, modelField.getText());
        
        saveClicked = true;
        dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void openSettingsFolder() {
        try {
            // Get the config directory from Config class
            Path configDir = config.getConfigDirectory();

            if (Files.exists(configDir)) {
                Desktop.getDesktop().open(configDir.toFile());
            } else {
                System.out.println("Configuration directory not found: " + configDir.toAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
