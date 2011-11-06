package org.brylex.maven.plugin;

import org.brylex.maven.plugin.jetty.EmbeddedJettyServer;
import org.sonatype.gshell.launcher.Configuration;

import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

public class EmbeddedConfiguration extends Configuration
{
    @Override
    public List<URL> getClassPath() throws Exception
    {
        List<URL> classPath = new ArrayList<URL>();

        ProtectionDomain domain = ExecutableGShellWarMojo.class.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();
        classPath.add(location);

        return classPath;
    }
}
