package dev.merosssany.calculatorapp.core.logging;

import dev.merosssany.calculatorapp.core.render.CleanupManager;
import dev.merosssany.calculatorapp.core.exception.VerboseException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private final String name;

    public Logger(String processName) {
        this.name = processName;
    }

    public void info(String ...messages) {
        System.out.println(format(LoggingLevel.INFO,messages));
    }
    @SafeVarargs
    public final <T> void info(T... objects) {
        System.out.println(formatObj(LoggingLevel.INFO,objects));
    }

    public void warn(String ...messages) {
        System.out.println("\033[43m"+format(LoggingLevel.WARN,messages)+"\033[0m");
    }
    public void warn(Object ...objects) {
        System.out.println("\033[43m"+formatObj(LoggingLevel.WARN,objects)+"\033[0m");
    }

    public void error(Throwable e, String ...messages) {
        System.err.println("\033[31m"+format(LoggingLevel.ERROR,messages)+"\033[0m");
        if (e instanceof VerboseException) {
            e.printStackTrace(); // Rely on VerboseException's custom output
        } else {
            printStacktrace(e, LoggingLevel.ERROR); // Use Logger's formatting
        }
    }
    public void error(Throwable e, Object ...objects) {
        System.err.println("\033[31m"+formatObj(LoggingLevel.ERROR,objects)+"\033[0m");
        if (e instanceof VerboseException) {
            e.printStackTrace(); // Rely on VerboseException's custom output
        } else {
            printStacktrace(e, LoggingLevel.ERROR); // Use Logger's formatting
        }
    }
    public void error(String ...messages) {
        System.err.println("\033[31m"+format(LoggingLevel.ERROR,messages)+"\033[0m");
    }
    public void error(Object ...objects) {
        System.err.println("\033[31m"+formatObj(LoggingLevel.ERROR,objects)+"\033[0m");
    }

    public void fatal(Throwable e,String ...messages) {
        System.err.println("\033[31m"+format(LoggingLevel.FATAL,messages)+"\033[0m");
        if (e instanceof VerboseException) {
            e.printStackTrace(); // Rely on VerboseException's custom output
        } else {
            printStacktrace(e, LoggingLevel.FATAL); // Use Logger's formatting
        }
        CleanupManager.createPopup("A Fatal error has been thrown: "+e.getMessage()+"\n"+formatStacktrace(e, LoggingLevel.FATAL));
    }
    public void fatal(Throwable e,Object ...objects) {
        System.err.println("\033[31m"+formatObj(LoggingLevel.FATAL,objects)+"\033[0m");
        if (e instanceof VerboseException) {
            e.printStackTrace(); // Rely on VerboseException's custom output
        } else {
            printStacktrace(e, LoggingLevel.FATAL); // Use Logger's formatting
        }
        CleanupManager.createPopup("A Fatal error has been thrown: "+e.getMessage()+"\n"+formatStacktrace(e, LoggingLevel.FATAL));
    }

    public void debug(Object ...objects) {
        System.out.println(formatObj(LoggingLevel.DEBUG,objects));
    }
    public void debug(String ...messages) {
        System.out.println(format(LoggingLevel.DEBUG,messages));
    }

    public void trace(String ...messages) {
        System.out.println(formatObj(LoggingLevel.TRACE,messages));
    }
    public void trace(Object ...objects) {
        System.out.println(formatObj(LoggingLevel.TRACE,objects));
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
        return  result.toString();
    }

    private <T> String format(LoggingLevel level, T[] messages) {
        StringBuilder result = new StringBuilder();
        result.append(formatTime(level));

        for (T message : messages) {
            result.append(message).append(" ");
        }
        return  result.toString();
    }

    private <T> String formatObj(LoggingLevel level,T[] objects) {
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
            if (!isPrimitiveClass(classFromObj)) {
                objectResult.append("Class: ")
                        .append(classFromObj.getName())
                        .append(" (")
                        .append(formatClassName(classFromObj))
                        .append(")\n");

                objectResult.append("\n");
                // Fields
                for (Field field : classFromObj.getFields()) {
                    try {
                        objectResult.append("    Field:\"")
                                .append(field.getName())
                                .append("\", Type:\"")
                                .append(field.getType())
                                .append(" (")
                                .append(formatClassName(field.getClass()))
                                .append(")\" = ")
                                .append(field.get(obj))
                                .append("\n")
                        ;
                    } catch (IllegalAccessException e) {
                        objectResult.append("    Field:\"")
                                .append(field.getName())
                                .append("\", Type:\"")
                                .append(field.getType())
                                .append(" (")
                                .append(formatClassName(field.getClass()))
                                .append(")\" = <IllegalAccessStateException>\n");
                    }
                }

                // Methods
                for (Method method : classFromObj.getMethods()) {
                    objectResult.append("    Method: ")
                            .append(method.getName())
                            .append("(");
                    for (Parameter parameter : method.getParameters()) {
                        objectResult.append(formatClassName(parameter.getType()))
                                .append(" ")
                                .append(parameter.getName())
                                .append(" ");
                    }
                    objectResult.append(")\n");
                }
            } else {
                textResult.append(obj).append(" ");
            }
        }

        finalResult = textResult.toString()+objectResult.toString();

        return finalResult;
    }

    private String formatTime(LoggingLevel level) {
        StringBuilder result = new StringBuilder();
        LocalDateTime time = LocalDateTime.now();
        String pattern = "yyyy-MM-dd HH:mm:ss";

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

    private void printStacktrace(Throwable e,LoggingLevel level) {
        System.out.println(formatStacktrace(e,level));
    }

    public String formatStacktrace(Throwable e,LoggingLevel level) {
        StringBuilder builder = new StringBuilder();
        builder.append(formatTime(level))
                .append("An exception has been occurred!")
                .append("   Message: ")
                .append(e.getMessage())
                .append("\n    Stacktrace:");
        for (StackTraceElement element : e.getStackTrace()) {
            if (element.isNativeMethod()) {
                builder.append("        [NATIVE METHOD] ")
                        .append(element.getClassName())
                        .append(" { ")
                        .append(element.getFileName())
                        .append(" } at line: ")
                        .append(element.getLineNumber())
                        .append(" > ")
                        .append(element.getMethodName())
                        .append("\n");
            } else {
                builder .append("       [METHOD] ")
                        .append(element.getClassName())
                        .append(" { ")
                        .append(element.getFileName())
                        .append(" } at line: ")
                        .append(element.getLineNumber())
                        .append(" inside ")
                        .append(element.getMethodName())
                        .append("\n");
            }
        }
        return "\033[31m"+ builder +"\033[0m";
    }

    public String formatClassName(Class<?> classProvided) {
        String fullName =  classProvided.getName();
        String packageName = classProvided.getPackageName();
        return  fullName.replaceAll(packageName + ".","");
    }

    protected static boolean isPrimitiveClass(Class<?> classProvided) {
        return classProvided.isPrimitive() || Number.class.isAssignableFrom(classProvided) || String.class.isAssignableFrom(classProvided) || Boolean.class.isAssignableFrom(classProvided);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public final <T extends Number> void info(T... numbers) {System.out.println(formatObj(LoggingLevel.INFO,numbers));}
}
