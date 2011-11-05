package org.brylex.maven.plugin;

import java.io.File;

public final class PluginUtils {

    private PluginUtils() {}

    public static File mkdirs(final File parentDir, final String path) {
        File targetDir = new File(parentDir, path);
        targetDir.mkdirs();
        return targetDir;
    }

}
