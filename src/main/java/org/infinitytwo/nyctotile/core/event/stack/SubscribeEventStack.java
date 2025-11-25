package org.infinitytwo.nyctotile.core.event.stack;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SubscribeEventStack {
//    int priority();
    String channel();
}
