package dev.merosssany.calculatorapp.core.event;

import java.lang.reflect.Method;
import java.util.*;

public abstract class EventBus {

    private static final Map<Class<?>, List<ListenerMethod>> listeners = new HashMap<>();

    public static void register(Object listenerInstance) {
        for (Method method : listenerInstance.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(SubscribeEvent.class)) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && Event.class.isAssignableFrom(params[0])) {
                    Class<?> eventType = params[0];
                    method.setAccessible(true);
                    listeners
                            .computeIfAbsent(eventType, k -> new ArrayList<>())
                            .add(new ListenerMethod(listenerInstance, method));
                }
            }
        }
    }

    public static void post(Event event) {
        List<ListenerMethod> methods = listeners.get(event.getClass());
        if (methods != null) {
            for (ListenerMethod lm : methods) {
                try {
                    lm.method.invoke(lm.instance, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private record ListenerMethod(Object instance, Method method) {}
}
