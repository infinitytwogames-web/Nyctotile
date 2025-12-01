package org.infinitytwo.nyctotile.core.event.stack;

import org.infinitytwo.nyctotile.core.exception.ChannelDoesNotExist;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Deprecated
public abstract class EventStack {
    private static final Map<String, SubscribeMethod> stackFlow = new HashMap<>();
    private static final ArrayList<String> channels = new ArrayList<>();
    private static int order = 0;

    public static void register(Object obj) throws ChannelDoesNotExist, SignatureException {
        Class<?> classAcquired = obj.getClass();
        Method[] methods = classAcquired.getMethods();

        for (Method method : methods) {
            SubscribeEventStack eventStack = method.getAnnotation(SubscribeEventStack.class);
            if (eventStack != null) {
                if (method.getParameters().length == 0) {
                    String channel = eventStack.channel();
                    if (channels.contains(channel)) {
                        order++;
                        stackFlow.put(channel + "-" + order, new SubscribeMethod(method, channel, obj, order));
                    } else {
                        throw new ChannelDoesNotExist("The channel '" + channel + "' does not exists");
                    }
                } else {
                    throw new SignatureException("@SubscribeEventStack does not need any parameters");
                }
            }
        }
    }

    public static void register(Class<?> classProvided) throws ChannelDoesNotExist, SignatureException {
        Method[] methods = classProvided.getMethods();

        for (Method method : methods) {
            SubscribeEventStack eventStack = method.getAnnotation(SubscribeEventStack.class);
            if (eventStack != null) {
                if (method.getParameters().length == 0) {
                    String channel = eventStack.channel();
                    if (channels.contains(channel)) {
                        order++;
                        stackFlow.put(channel + "-" + order, new SubscribeMethod(method, channel, null, order));
                    } else {
                        throw new ChannelDoesNotExist("The channel '" + channel + "' does not exists");
                    }
                } else {
                    throw new SignatureException("@SubscribeEventStack does not need any parameters");
                }
            }
        }
    }

    public static void register(Method method, String Channel) throws ChannelDoesNotExist, SignatureException {
        SubscribeEventStack eventStack = method.getAnnotation(SubscribeEventStack.class);
        if (eventStack != null) {
            if (method.getParameters().length == 0) {
                String channel = eventStack.channel();
                if (channels.contains(channel)) {
                    order++;
                    stackFlow.put(channel + "-" + order, new SubscribeMethod(method, channel, null, order));
                } else {
                    throw new ChannelDoesNotExist("The channel '" + channel + "' does not exists");
                }
            } else {
                throw new SignatureException("@SubscribeEventStack does not need any parameters");
            }
        }
    }

    public static void registerChannel(String channel) {
        if (!channels.contains(channel)) channels.add(channel);
    }

    public static RunResult run(String channel) {
        int total = 0;
        int running = 0;
        int failed = 0;
        ArrayList<Exception> exceptions = new ArrayList<>();

        for (SubscribeMethod method : stackFlow.values()) {
            if (!Objects.equals(method.channel, channel)) continue;
            total++;

            try {
                method.method.invoke(method.instance);
                running++;
            } catch (IllegalAccessException | InvocationTargetException e) {
                failed++;
                exceptions.add(e);
            }
        }

        return new RunResult(running,total,failed, exceptions.toArray(new Exception[0]));
    }

    public static void cleanup() {
        channels.clear();
        stackFlow.clear();
    }

    private static class SubscribeMethod {
        public Method method;
        public String channel;
        public Object instance;
        public int order; // That could be useful for future

        public SubscribeMethod(Method method, String channel, Object instance, int order) {
            this.method = method;
            this.channel = channel;
            this.instance = instance;
            this.order = order;
        }
    }

    public static class RunResult {
        public int successfulRuns;
        public int totalRuns;
        public int failedRuns;
        public Exception[] exceptions;

        public RunResult(int successfulRuns, int totalRuns, int failedRuns, Exception[] exceptions) {
            this.successfulRuns = successfulRuns;
            this.totalRuns = totalRuns;
            this.failedRuns = failedRuns;
            this.exceptions = exceptions;
        }
    }

    public static String[] getChannels() {
        return channels.toArray(new String[0]);
    }
}
