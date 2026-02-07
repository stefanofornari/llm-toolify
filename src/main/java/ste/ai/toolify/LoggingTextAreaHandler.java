package ste.ai.toolify;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import javafx.scene.control.TextArea;

public class LoggingTextAreaHandler extends Handler {

    private final String loggerName;
    private final TextArea textArea;

    public LoggingTextAreaHandler(final String loggerName, final TextArea textArea) {
        this.loggerName = loggerName;
        this.textArea = textArea;
        setFormatter(new java.util.logging.Formatter() {
            @Override
            public String format(LogRecord record) {
                return "----------\n" +
                        record.getLoggerName() + "\n" +
                        record.getParameters() + "\n" +
                        record.getMessage() + "\n";
            }
        });
    }

    @Override
    public void publish(final LogRecord record) {
        //if (!loggerName.equals(record.getLoggerName()) || !isLoggable(record)) {
        //    return;
        //}

        String message = getFormatter().format(record);
        textArea.appendText(message);
    }

    @Override
    public void flush() {
        // No need to implement for this handler
    }

    @Override
    public void close() throws SecurityException {
        // No need to implement for this handler
    }
}
