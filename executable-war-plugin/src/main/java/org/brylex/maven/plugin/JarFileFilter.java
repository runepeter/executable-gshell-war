package org.brylex.maven.plugin;

import java.io.File;
import java.io.FileFilter;

public class JarFileFilter implements FileFilter {
    @Override
    public boolean accept(File file) {
        System.err.println(file.getName());
        return file != null && file.getName().endsWith(".jar");
    }
}
