package ste.ai.toolify;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigController {

    @FXML private TextField endpointField;
    @FXML private TextField keyField;
    @FXML private TextField modelField;
    @FXML private TextField workingDirField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button browseButton;
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
        workingDirField.setText(config.entry(Config.WORKING_DIR));
    }

    @FXML
    private void handleSave() {
        config.entry(Config.ENDPOINT, endpointField.getText());
        config.entry(Config.API_KEY, keyField.getText());
        config.entry(Config.MODEL_NAME, modelField.getText());
        config.entry(Config.WORKING_DIR, workingDirField.getText());
        saveClicked = true;
        dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void browseDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Working Directory");
        File selectedDirectory = directoryChooser.showDialog(dialogStage);
        if (selectedDirectory != null) {
            workingDirField.setText(selectedDirectory.getAbsolutePath());
        }
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
            Path configDir = config.getConfigDirectory();
            if (Files.exists(configDir)) {
                Desktop.getDesktop().open(configDir.toFile());
            } else {
                System.err.println("Configuration directory not found: " + configDir.toAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}