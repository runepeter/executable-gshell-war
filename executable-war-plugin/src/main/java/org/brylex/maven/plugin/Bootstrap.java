package org.brylex.maven.plugin;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class Bootstrap
{
    private final Class<?> mainClass;
    private final String groupId;
    private final String artifactId;
    private final String version;

    private String applicationName = null;

    public Bootstrap(final Class<?> mainClass, final String groupId, final String artifactId, final String version)
    {
        this.mainClass = mainClass;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public void setApplicationName(String applicationName)
    {
        this.applicationName = applicationName;
    }

    public void toDir(final File dir)
    {
        toFile(new File(dir, "bootstrap.properties"));
    }

    public void toFile(final File file)
    {

        Properties properties = new Properties();
        properties.put("shell.main", mainClass.getName());
        properties.put("shell.program", applicationName);
        properties.put("shell.home", ".");
        properties.put("shell.version", version);
        properties.put("project.groupId", groupId);
        properties.put("project.artifactId", artifactId);
        properties.put("project.version", version);
        properties.put("application.name", applicationName);

        FileWriter writer = null;
        try
        {
            writer = new FileWriter(file);
            properties.store(writer, "utf-8");

        } catch (IOException e)
        {
            throw new RuntimeException("Unable to write bootstrap properties to file " + file + ".");
        } finally
        {
            IOUtils.closeQuietly(writer);
        }
    }

}
