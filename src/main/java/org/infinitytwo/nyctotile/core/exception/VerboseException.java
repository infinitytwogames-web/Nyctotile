package org.infinitytwo.nyctotile.core.exception;

public class VerboseException extends Throwable {
    private String description;

    public VerboseException(String message, String description) {
        super(message);
    }

    @Override
    public void printStackTrace() {
        StringBuilder builder = new StringBuilder();
        builder.append("An exception has been occurred!")
                .append("   Message: ")
                .append(this.getMessage())
                .append("\n    Stacktrace:");
        for (StackTraceElement element : this.getStackTrace()) {
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
                builder.append("       [METHOD] ")
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
        System.out.println("\033[31m" + builder + "\033[0m");
    }
}
