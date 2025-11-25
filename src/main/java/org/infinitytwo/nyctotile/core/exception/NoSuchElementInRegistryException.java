package org.infinitytwo.nyctotile.core.exception;

public class NoSuchElementInRegistryException extends Throwable {

    public NoSuchElementInRegistryException(String s) {
        super(s);
    }

    public NoSuchElementInRegistryException(String s, Exception e) {
        super(s,e);
    }


    public NoSuchElementInRegistryException(Exception e) {
        super(e);
    }
}
