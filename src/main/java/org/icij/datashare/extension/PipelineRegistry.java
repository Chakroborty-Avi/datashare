package org.icij.datashare.extension;

import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.text.nlp.Pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static java.util.Optional.ofNullable;

public class PipelineRegistry {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final Path pluginDir;
    @org.jetbrains.annotations.NotNull
    private final PropertiesProvider propertiesProvider;
    private Map<Pipeline.Type, Pipeline> pipelines = new HashMap<>();

    public PipelineRegistry(PropertiesProvider propertiesProvider) {
        this.pluginDir = Paths.get(propertiesProvider.get(PropertiesProvider.PLUGINS_DIR).orElse("./plugins"));
        this.propertiesProvider = propertiesProvider;
    }

    public Pipeline get(Pipeline.Type type) {
        return pipelines.get(type);
    }

    public Set<Pipeline.Type> getPipelineTypes() {
        return pipelines.keySet();
    }

    public synchronized void load() {
        File[] jars = ofNullable(pluginDir.toFile().listFiles((file, s) -> s.endsWith(".jar"))).
                orElseThrow(() -> new IllegalStateException("invalid path for plugins: " + pluginDir));
        URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        for (File jar : jars) {
            try {
                Class<?> pipelineClassInJar = findClassesInJar(Pipeline.class, jar.toString());
                if (pipelineClassInJar != null) {
                    LOGGER.info("adding pipeline {} to system classloader", pipelineClassInJar);
                    Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                    method.setAccessible(true);
                    method.invoke(classLoader, classLoader.getResource(jar.toString())); // hack to load jar
                    Pipeline abstractPipeline = (Pipeline) pipelineClassInJar.getDeclaredConstructor(PropertiesProvider.class).newInstance(propertiesProvider);
                    pipelines.put(abstractPipeline.getType(), abstractPipeline);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException | InstantiationException | ClassNotFoundException e) {
                LOGGER.error("Cannot load jar " + jar, e);
            }
        }
    }

    public synchronized Class<?> findClassesInJar(final Class<?> baseInterface, final String jarName) throws IOException, ClassNotFoundException {
        final String jarFullPath = File.separator + jarName;
        final ClassLoader classLoader = getClass().getClassLoader();
        final URL url = new URL("jar:file:" + jarFullPath + "!/");
        URLClassLoader ucl = new URLClassLoader(new URL[]{url}, classLoader);
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFullPath));
        for (JarEntry jarEntry = jarInputStream.getNextJarEntry(); jarEntry != null; jarEntry = jarInputStream.getNextJarEntry()) {
            if (jarEntry.getName().endsWith(".class")) {
                String classname = jarEntry.getName().replaceAll("/", "\\.");
                classname = classname.substring(0, classname.length() - 6);
                if (!classname.contains("$")) {
                    final Class<?> myLoadedClass = Class.forName(classname, true, ucl);
                    if (baseInterface.isAssignableFrom(myLoadedClass)) {
                        return myLoadedClass;
                    }
                }
            }
        }
        return null;
    }
}

