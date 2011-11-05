package org.brylex.maven.plugin;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class AbstractMain {

    protected AbstractMain(final String bootstrapClassName) {
        JarClassLoader serverClassLoader = createClassLoader();
        Thread.currentThread().setContextClassLoader(serverClassLoader);

        try {
            Class<?> bootstrapClass = serverClassLoader.loadClass(bootstrapClassName, true);

            ProtectionDomain domain = bootstrapClass.getProtectionDomain();
            URL location = domain.getCodeSource().getLocation();

            invokeClass(bootstrapClass.getName(), new String[]{location.toString()});
        } catch (Throwable e) {
            throw new RuntimeException("Unable to launch bootstrap class [" + bootstrapClassName + "].", e);
        }
    }

    public static void invokeClass(String className, String[] args) throws Exception {
        Method method = getMainMethod(className, args);
        method.invoke(null, new Object[]{args});
    }

    private static Method getMainMethod(String className, String[] args) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);

        Method method = clazz.getMethod("main", new Class[]{args.getClass()});
        method.setAccessible(true);
        int modifiers = method.getModifiers();
        if (method.getReturnType() != void.class || !Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            throw new NoSuchMethodException("main");
        }

        return method;
    }

    protected static JarClassLoader createClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();

        return new JarClassLoader(systemClassLoader.getURLs(), contextClassLoader);
    }

    public static class JarClassLoader extends URLClassLoader {
        private static void close(Closeable closeable) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private static boolean isServerJar(String fileName) {
            return fileName != null && fileName.startsWith("WEB-INF/server/") && fileName.toLowerCase().endsWith(".jar");
        }

        private static File jarEntryAsFile(JarFile jarFile, JarEntry jarEntry) throws IOException {
            InputStream input = null;
            OutputStream output = null;
            try {
                String name = jarEntry.getName().replace('/', '_');
                int i = name.lastIndexOf(".");
                String extension = i > -1 ? name.substring(i) : "";
                File file = File.createTempFile(name.substring(0, name.length() - extension.length()) + ".", extension);
                file.deleteOnExit();
                input = jarFile.getInputStream(jarEntry);
                output = new FileOutputStream(file);
                int readCount;
                byte[] buffer = new byte[4096];
                while ((readCount = input.read(buffer)) != -1) {
                    output.write(buffer, 0, readCount);
                }
                return file;
            } finally {
                close(input);
                close(output);
            }
        }

        public JarClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
            try {
                ProtectionDomain protectionDomain = getClass().getProtectionDomain();
                CodeSource codeSource = protectionDomain.getCodeSource();
                URL rootJarUrl = codeSource.getLocation();

                File warFile = new File(rootJarUrl.getPath());
                JarFile jarFile = new JarFile(warFile);
                Enumeration<JarEntry> jarEntries = jarFile.entries();
                while (jarEntries.hasMoreElements()) {
                    JarEntry jarEntry = jarEntries.nextElement();
                    if (!jarEntry.isDirectory() && isServerJar(jarEntry.getName())) {
                        addJarResource(jarEntryAsFile(jarFile, jarEntry));
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException("Unable to instantiate server classloader.");
            }
        }

        private void addJarResource(File file) throws IOException {
            JarFile jarFile = new JarFile(file);
            addURL(file.toURL());

            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();
                if (!jarEntry.isDirectory() && isServerJar(jarEntry.getName())) {
                    addJarResource(jarEntryAsFile(jarFile, jarEntry));
                }
            }
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            try {
                Class<?> clazz = findLoadedClass(name);
                if (clazz == null) {
                    clazz = findClass(name);
                    if (resolve)
                        resolveClass(clazz);
                }
                return clazz;
            } catch (ClassNotFoundException e) {
                return super.loadClass(name, resolve);
            }
        }
    }
}
