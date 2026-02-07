package ste.ai.toolify.log;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LogViewerHandler extends Handler {

    private final String loggerName;
    private final LogViewer logViewer;

    public LogViewerHandler(final String loggerName, final LogViewer logViewer) {
        this.loggerName = loggerName;
        this.logViewer = logViewer;
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
        if (loggerName != null && !record.getLoggerName().startsWith(loggerName) || !isLoggable(record)) {
            return;
        }
        
        System.out.println("1>" + record.getMessage());

        logViewer.log(getFormatter().format(record));
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
