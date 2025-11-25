package org.infinitytwo.nyctotile.core.data.stream;

import org.jetbrains.annotations.NotNull;

import java.io.*;

public class LogStream extends FileOutputStream {
    public LogStream(@NotNull File file) throws FileNotFoundException {
        super(file);
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);

        System.out.println((char) b);
    }
}
