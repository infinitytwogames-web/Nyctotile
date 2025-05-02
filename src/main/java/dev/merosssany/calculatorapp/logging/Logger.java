package dev.merosssany.calculatorapp.logging;

import dev.merosssany.calculatorapp.core.CleanupManager;

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

    public void log(String ...messages) {
        System.out.println(format(LoggingLevel.INFO,messages));
    }
    public void log(Object ...objects) {
        System.out.println(formatObj(LoggingLevel.INFO,objects));
    }

    public void warn(String ...messages) {
        System.out.println(format(LoggingLevel.WARN,messages));
    }
    public void warn(Object ...objects) {
        System.out.println(formatObj(LoggingLevel.WARN,objects));
    }

    public void error(String ...messages) {
        System.out.println(format(LoggingLevel.ERROR,messages));
    }
    public void error(Object ...objects) {
        System.out.println(formatObj(LoggingLevel.ERROR,objects));
    }

    public void fatal(Throwable e,String ...messages) {
        System.out.println(format(LoggingLevel.FATAL,messages));
        e.printStackTrace();
        CleanupManager.exit(1);
    }
    public void fatal(Throwable e,Object ...objects) {
        System.out.println(formatObj(LoggingLevel.FATAL,objects));
        e.printStackTrace();
        CleanupManager.exit(1);
    }

    public void debug(Object ...objects) {
        System.out.println(formatObj(LoggingLevel.WARN,objects));
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

    private String formatObj(LoggingLevel level,Object[] objects) {
        if (objects.length == 0) return "";

        StringBuilder objectResult = new StringBuilder();
        StringBuilder textResult = new StringBuilder();
        objectResult.append(formatTime(level));

        String finalResult = "";

        for (Object obj : objects) {
            Class<?> classFromObj = obj.getClass();
            if (!classFromObj.getName().equals(String.class.getName()) ||
                    !classFromObj.getName().equals(boolean.class.getName()) ||
                    !classFromObj.getName().equals(int.class.getName()) ||
                    !classFromObj.getName().equals(long.class.getName()) ||
                    !classFromObj.getName().equals(float.class.getName()) ||
                    !classFromObj.getName().equals(char.class.getName())
            ) {
                objectResult.append("Class: ")
                        .append(classFromObj.getName())
                        .append(" (")
                        .append(formatClassName(classFromObj))
                        .append(")\n");

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
                                .append(field.get(classFromObj));
                    } catch (IllegalAccessException e) {
                        objectResult.append("    Field:\"")
                                .append(field.getName())
                                .append("\", Type:\"")
                                .append(field.getType())
                                .append(" (")
                                .append(formatClassName(field.getClass()))
                                .append(")\" = <IllegalAccessStateException>");
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

        finalResult = textResult.toString()+"\n"+objectResult.toString();

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
                .append("/")
                .append(level)
                .append("]: ");

        return result.toString();
    }

    private String formatClassName(Class<?> classProvided) {
        String fullName =  classProvided.getName();
        String packageName = classProvided.getPackageName();
        return  fullName.replaceAll(packageName + ".","");
    }
}
