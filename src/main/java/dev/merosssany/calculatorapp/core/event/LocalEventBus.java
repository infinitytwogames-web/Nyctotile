package dev.merosssany.calculatorapp.core.event;

import dev.merosssany.calculatorapp.core.logging.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalEventBus {
    private static final Map<Class<?>, List<ListenerMethod>> listeners = new HashMap<>();
    private final Logger logger;

    public LocalEventBus(String name) {
        this.logger = new Logger("EventBus "+name);
        logger.info("Created new EventBus");
    }

    public void register(Object listenerInstance) {
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

    public void post(Event event) {
        List<ListenerMethod> methods = listeners.get(event.getClass());
        if (methods != null) {
            for (ListenerMethod lm : methods) {
                try {
                    lm.method.invoke(lm.instance, event);
                } catch (Exception e) {
                    logger.error(e,"Cannot post event:",logger.formatClassName(event.getClass()));
                }
            }
        }
    }

    private record ListenerMethod(Object instance, Method method) {}
}
