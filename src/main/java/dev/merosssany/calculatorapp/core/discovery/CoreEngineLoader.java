package dev.merosssany.calculatorapp.core.discovery;

import dev.merosssany.calculatorapp.core.Core;
import dev.merosssany.calculatorapp.core.event.stack.EventStack;
import dev.merosssany.calculatorapp.core.exception.ChannelDoesNotExist;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CoreEngineLoader {
    private static List<Class<?>> classes;

    public static List<Class<?>> findAnnotatedClassesFromJar() {
        List<Class<?>> modClasses = new ArrayList<>();
        try {
            // Get the path of the running JAR
            String jarPath = CoreEngineLoader.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            try (JarFile jarFile = new JarFile(jarPath)) {
                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();

                    // Only look for class files
                    if (name.endsWith(".class") && !name.contains("$")) {
                        String className = name
                                .replace('/', '.')
                                .substring(0, name.length() - 6);

                        try {
                            Class<?> cls = Class.forName(className);
                            if (cls.isAnnotationPresent(Core.class)) {
                                modClasses.add(cls);
                            }
                        } catch (Throwable t) {
                            // Might fail on unrelated classes — ignore
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return modClasses;
    }

    public static List<Class<?>> findAnnotatedModClassesFromFS() {
        List<Class<?>> modClasses = new ArrayList<>();
        String basePath = "dev/merosssany"; // Your root package
        File root = new File("build/classes/java/main/" + basePath);

        if (!root.exists()) {
            System.err.println("Build output not found: " + root.getAbsolutePath());
            return modClasses;
        }

        try {
            Files.walk(root.toPath())
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> {
                        String relative = root.toPath().relativize(path).toString();
                        String className = ("dev.merosssany." + relative)
                                .replace(File.separatorChar, '.')
                                .replaceAll("\\.class$", "");

                        try {
                            Class<?> cls = Class.forName(className);
                            if (cls.isAnnotationPresent(Core.class)) {
                                modClasses.add(cls);
                            }
                        } catch (Throwable t) {
                            System.err.println("Failed to load class " + className);
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return modClasses;
    }

    public static List<Class<?>> findAnnotatedClasses() {
        // Detect if running from JAR or classes dir
        URL codeSource = CoreEngineLoader.class.getProtectionDomain().getCodeSource().getLocation();
        if (codeSource.getPath().endsWith(".jar")) {
            return findAnnotatedClassesFromJar();  // scan JAR
        } else {
            return findAnnotatedModClassesFromFS();   // scan filesystem (Gradle)
        }
    }

    public static void enqueueWork(String Package, String channel, String methodName) {
        if (classes != null) classes = findAnnotatedModClassesFromFS();

        try {
            Method method;
            if (classes != null) {
                method = classes.getClass().getDeclaredMethod(methodName);
                if (Modifier.isStatic(method.getModifiers())) {
                    EventStack.register(method,channel);
                }
            }
        } catch (NoSuchMethodException  e) {
            throw new RuntimeException(e);
        } catch (ChannelDoesNotExist e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }
}
