package ste.ai.toolify;

import dev.dirs.ProjectDirectories;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.configuration2.JSONConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class Config {

    public static final String API_KEY = "key";
    public static final String MODEL_NAME = "model";
    public static final String ENDPOINT = "endpoint";

    private final JSONConfiguration config; // Use CompositeConfiguration
    private final Path configFile; 
    private final FileBasedConfigurationBuilder<JSONConfiguration> jsonBuilder; // To access the file config directly for saving

    public Config() {
        ProjectDirectories projDirs = ProjectDirectories.from("ste", "", "toolify");
        String configDir = projDirs.configDir;
        configFile = Paths.get(configDir, "config.json"); 
        
        try {
            if (!Files.exists(configFile.getParent())) {
                Files.createDirectories(configFile.getParent());
            }

            Parameters params = new Parameters();
            jsonBuilder = new FileBasedConfigurationBuilder<>(JSONConfiguration.class)
                .configure(params.fileBased().setFile(configFile.toFile()));
            jsonBuilder.setAutoSave(true); // Enable auto-saving changes to the file
            
            if (!Files.exists(configFile)) {
                Files.writeString(
                    configFile, 
                    """
                    {
                        "%s":"%s",
                        "%s":"%s",
                        "%s":"%s"
                    }
                    """.formatted(
                        API_KEY, "your-openai-api-key-here",
                        MODEL_NAME, "gpt-3.5-turbo",
                        ENDPOINT, "https://api.openai.com/v1/chat/completions"
                    )
                );
            }
            config = jsonBuilder.getConfiguration();
        } catch (ConfigurationException|IOException e) {
            throw new RuntimeException("Could not load configuration", e);
        }
    }

    public String entry(String key) {
        return config.getString(key);
    }

    public void entry(String key, String value) {
        try {
            config.setProperty(key, value);
            jsonBuilder.save(); // Explicitly save changes to the file
        } catch (ConfigurationException e) {
            System.err.println("Error saving configuration: " + e.getMessage());
        }
    }

    public Path getConfigDirectory() {
        return configFile.getParent();
    }
}
