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
            public String format(final LogRecord record) {
                return new HTTPLogParser().json(record.getMessage()).toString();
            }
        });
    }

    @Override
    public void publish(final LogRecord record) {
        if (loggerName != null && !record.getLoggerName().startsWith(loggerName) || !isLoggable(record)) {
            return;
        }

        //
        // Capturing any throwable so that any other handler will kepp processing
        // (if this would not be done, the exception will raise up and next
        // handlers will not execute)
        //
        try {
            logViewer.log(getFormatter().format(record));
        } catch (Throwable x) {
            x.printStackTrace();
        }
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
