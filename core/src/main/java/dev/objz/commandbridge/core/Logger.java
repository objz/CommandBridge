package dev.objz.commandbridge.core;

import org.slf4j.LoggerFactory;

public class Logger {
    private final org.slf4j.Logger logger;
    private Boolean debug;

    public Logger(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Logger name cannot be null or empty");
        }
        this.logger = LoggerFactory.getLogger(name);
        this.debug = false;
    }

    public Logger() {
        this("Logger");
    }

    public void info(String message, Object... args) {
        log("INFO", message, false, args);
    }

    public void warn(String message, Object... args) {
        log("WARN", message, debug, args);
    }

    public void error(String message, Object... args) {
        log("ERROR", message, debug, args);
    }

    public void debug(String message, Object... args) {
        if (debug) {
            log("DEBUG", message, true, args);
        }
    }

    private void log(String level, String message, boolean extended, Object... args) {
        String formattedMessage = getString(message, extended);

        switch (level) {
            case "INFO":
                logger.info(formattedMessage, args);
                break;
            case "WARN":
                logger.warn(formattedMessage, args);
                break;
            case "ERROR":
                logger.error(formattedMessage, args);
                break;
            default:
                logger.info(formattedMessage, args); // Fallback
                break;
        }
    }

    private static String getString(String message, boolean extended) {
        String formattedMessage;
        if (extended) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StackTraceElement caller = stackTrace[4];
            String className = caller.getClassName();
            String methodName = caller.getMethodName();

            formattedMessage = String.format(
                    "(%s#%s): %s",
                    className,
                    methodName,
                    message);

        } else {
            formattedMessage = message;
        }
        return formattedMessage;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public Boolean getDebug() {
        return debug;
    }
}
