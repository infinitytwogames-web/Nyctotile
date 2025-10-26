package org.infinitytwo.umbralore.core.logging;

import org.infinitytwo.umbralore.core.Main;
import org.infinitytwo.umbralore.core.RGB;
import org.infinitytwo.umbralore.core.renderer.CleanupManager;
import org.infinitytwo.umbralore.core.exception.VerboseException;
import org.infinitytwo.umbralore.core.ui.UI;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class Logger {
    private final String name;
//    private final FileOutputStream stream;

    public Logger(String processName) {
        this.name = processName;
        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";

//        File file = Path.of("logs",LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern))).toFile();
//        try {
//            stream = new FileOutputStream(file, true);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    private void log(String message) {
        System.out.println(message);
//        try {
//            stream.write((message+"\n").getBytes(StandardCharsets.UTF_8));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    public void info(String... messages) {
        log(format(LoggingLevel.INFO, messages));
    }

    @SafeVarargs
    public final <T> void info(T... objects) {
        log(formatObj(LoggingLevel.INFO, objects));
    }

    public void warn(String... messages) {
        log("\033[43m" + format(LoggingLevel.WARN, messages) + "\033[0m");
    }

    public void warn(Object... objects) {
        log("\033[43m" + formatObj(LoggingLevel.WARN, objects) + "\033[0m");
    }

    public void error(Throwable e, String... messages) {
        System.err.println("\033[31m" + format(LoggingLevel.ERROR, messages) + "\033[0m");
        if (e instanceof VerboseException) {
            e.printStackTrace(); // Rely on VerboseException's custom output
        } else {
            printStacktrace(e, LoggingLevel.ERROR); // Use Logger's formatting
        }
    }

    public void error(Throwable e, Object... objects) {
        System.err.println("\033[31m" + formatObj(LoggingLevel.ERROR, objects) + "\033[0m");
        if (e instanceof VerboseException) {
            e.printStackTrace(); // Rely on VerboseException's custom output
        } else {
            printStacktrace(e, LoggingLevel.ERROR); // Use Logger's formatting
        }
    }

    public void error(String... messages) {
        System.err.println("\033[31m" + format(LoggingLevel.ERROR, messages) + "\033[0m");
    }

    public void error(Object... objects) {
        System.err.println("\033[31m" + formatObj(LoggingLevel.ERROR, objects) + "\033[0m");
    }

    public void fatal(Throwable e, String... messages) {
        System.err.println("\033[31m" + format(LoggingLevel.FATAL, messages) + "\033[0m");
        if (e instanceof VerboseException) {
            e.printStackTrace(); // Rely on VerboseException's custom output
        } else {
            printStacktrace(e, LoggingLevel.FATAL); // Use Logger's formatting
        }
        CleanupManager.createPopup("A Fatal error has been thrown: " + e.getMessage() + "\n" + formatStacktrace(e));
        Main.cleanup();
    }

    public void fatal(Throwable e, Object... objects) {
        System.err.println("\033[31m" + formatObj(LoggingLevel.FATAL, objects) + "\033[0m");
        if (e instanceof VerboseException) {
            e.printStackTrace(); // Rely on VerboseException's custom output
        } else {
            printStacktrace(e, LoggingLevel.FATAL); // Use Logger's formatting
        }
        CleanupManager.createPopup("A Fatal error has been thrown: " + e.getMessage() + "\n" + formatStacktrace(e));
        Main.cleanup();
    }

    public void debug(Object... objects) {
        log(formatObj(LoggingLevel.DEBUG, objects));
    }

    public void debug(String... messages) {
        log(format(LoggingLevel.DEBUG, messages));
    }

    public void trace(String... messages) {
        log(format(LoggingLevel.TRACE, messages));
    }

    public void trace(Object... objects) {
        log(formatObj(LoggingLevel.TRACE, objects));
    }

    public String getProcessName() {
        return name;
    }

    private String format(LoggingLevel level, String[] messages) {
        StringBuilder result = new StringBuilder();
        result.append(formatTime(level));

        for (String message : messages) {
            result.append(message).append(" ");
        }
        return result.toString();
    }

    private <T> String formatObj(LoggingLevel level, T[] objects) {
        if (objects.length == 0) return "";

        StringBuilder objectResult = new StringBuilder();
        StringBuilder textResult = new StringBuilder();
        textResult.append(formatTime(level));

        String finalResult = "";

        for (Object obj : objects) {
            if (obj == null) {
                textResult.append("<NULL> ");
                continue;
            }

            Class<?> classFromObj = obj.getClass();
            if (!isPrimitiveClass(classFromObj) && !isSupported(classFromObj)) {
                objectResult.append("Class: ")
                        .append(classFromObj.getName())
                        .append(" (")
                        .append(formatClassName(classFromObj))
                        .append(")\n")
                        .append(obj)
                ;
            } else {
                textResult.append(obj).append(" ");
            }
        }

        finalResult = textResult.toString() + objectResult.toString();

        return finalResult;
    }

    private String formatTime(LoggingLevel level) {
        StringBuilder result = new StringBuilder();
        LocalDateTime time = LocalDateTime.now();
        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        result.append("[")
                .append(time.format(formatter))
                .append("] [")
                .append(this.name)
                .append(".")
                .append(Thread.currentThread().getName())
                .append("/")
                .append(level)
                .append("]: ");

        return result.toString();
    }

    private void printStacktrace(Throwable e, LoggingLevel level) {
        log(formatStacktrace(e));
    }

    public String formatStacktrace(Throwable e) {
        StringBuilder builder = new StringBuilder();
        builder
                .append("An exception has been occurred!\n")
                .append("   Message: ")
                .append(e.getMessage() == null ? "<No Message (null)>" : e.getMessage())
                .append("\n   Stacktrace:\n");
        for (StackTraceElement element : e.getStackTrace()) {
            if (element.isNativeMethod()) {
                builder.append("       [NATIVE METHOD] ")
                        .append(element.getClassName())
                        .append(" {")
                        .append(element.getFileName())
                        .append("} at line: ")
                        .append(element.getLineNumber())
                        .append(" inside ")
                        .append(element.getMethodName())
                        .append("\n");
            } else {
                builder.append("       [METHOD] ")
                        .append(element.getClassName())
                        .append(" {")
                        .append(element.getFileName())
                        .append("} at line: ")
                        .append(element.getLineNumber())
                        .append(" inside ")
                        .append(element.getMethodName())
                        .append("\n");
            }
        }
        return "\033[31m" + builder + "\033[0m";
    }

    public String formatClassName(Class<?> classProvided) {
        String fullName = classProvided.getName();
        String packageName = classProvided.getPackageName();
        return fullName.replaceAll(packageName + ".", "");
    }

    private static boolean isPrimitiveClass(Class<?> classProvided) {
        return classProvided.isPrimitive() || Number.class.isAssignableFrom(classProvided) || String.class.isAssignableFrom(classProvided) || Boolean.class.isAssignableFrom(classProvided);
    }

    private static boolean isSupported(Class<?> clazz) {
        if (clazz.isAssignableFrom(RGB.class)) return true;
        else return clazz.isAssignableFrom(UI.class);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public final <T extends Number> void info(T... numbers) {
        log(formatObj(LoggingLevel.INFO, numbers));
    }
}
