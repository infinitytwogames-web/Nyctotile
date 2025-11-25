package org.infinitytwo.nyctotile.core.event.bus;

import org.infinitytwo.nyctotile.core.event.Event;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class EventBus {
    private static final EventBus global = new EventBus("Global");
    private final Map<Class<?>, List<ListenerMethod>> subscribers = new HashMap<>();
    private final String name;

    public static void dispatch(Event event) {
        global.post(event);
    }

    public static void connect(Object obj) {
        global.register(obj);
    }

    public static void disconnect(Object obj) {
        global.unregister(obj);
    }

    public static void connect(Class<?> listenerClass) {
        if (listenerClass == null) {
            System.err.println("EventBus: Cannot register null class for static methods.");
            return;
        }

        for (Method method : listenerClass.getDeclaredMethods()) {
            // Only process static methods for static registration
            if (Modifier.isStatic(method.getModifiers()) && method.isAnnotationPresent(SubscribeEvent.class)) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && Event.class.isAssignableFrom(params[0])) {
                    Class<?> eventType = params[0];
                    method.setAccessible(true); // Allow access to private static methods
                    global.subscribers
                            .computeIfAbsent(eventType, k -> new ArrayList<>())
                            .add(new ListenerMethod(null, method)); // Pass null for instance for static methods
                }
            }
        }
    }

    public EventBus(String process) {
        name = process;
    }

    public void register(Object listenerInstance) {
        if (listenerInstance == null) return;

        Class<?> clazz = listenerInstance.getClass();

        // --- FIX: Traverse the class hierarchy to find inherited @SubscribeEvent methods ---
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(SubscribeEvent.class)) {
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length == 1 && Event.class.isAssignableFrom(params[0])) {
                        Class<?> eventType = params[0];

                        // Check if the method is static (should be handled by the other register overload)
                        if (!Modifier.isStatic(method.getModifiers())) {
                            method.setAccessible(true);
                            subscribers
                                    .computeIfAbsent(eventType, k -> new ArrayList<>())
                                    .add(new ListenerMethod(listenerInstance, method));
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass(); // Move up to the superclass
        }
    }

    public void post(Event event) {
        Class<?> eventType = event.getClass();

        // Collect listeners for the event class AND all its superclasses/interfaces
        List<ListenerMethod> collectedMethods = new ArrayList<>();
        Set<Class<?>> classesToSearch = new HashSet<>();

        // Start by adding the exact event class
        classesToSearch.add(eventType);

        // Add all superclasses and interfaces up to (but excluding) Object
        Class<?> currentClass = eventType;
        while (currentClass != null && currentClass != Object.class) {
            classesToSearch.add(currentClass);

            // Add all implemented interfaces (e.g., if you have interfaces like Cancellable)
            classesToSearch.addAll(Arrays.asList(currentClass.getInterfaces()));

            currentClass = currentClass.getSuperclass();
        }

        // Search the listener map using the collected class types
        for (Class<?> type : classesToSearch) {
            List<ListenerMethod> methods = subscribers.get(type);
            if (methods != null) {
                collectedMethods.addAll(methods);
            }
        }

        if (!collectedMethods.isEmpty()) {
            // Use a new list to prevent ConcurrentModificationException if a listener unregisters itself
            for (ListenerMethod lm : new ArrayList<>(collectedMethods)) {
                try {
                    lm.method.invoke(lm.instance, event);
                } catch (Exception e) {
                    // Wrap in RuntimeException for propagation
                    throw new RuntimeException("Error invoking listener for event " + event.getClass().getSimpleName(), e);
                }
            }
        }
    }

    public void unregister(Object listenerInstance) {
        if (listenerInstance == null) return;

        for (List<ListenerMethod> methods : subscribers.values()) {
            methods.removeIf(lm -> lm.instance == listenerInstance);
        }
    }

    public String getProcess() {
        return name;
    }

    private record ListenerMethod(Object instance, Method method) {}
}
