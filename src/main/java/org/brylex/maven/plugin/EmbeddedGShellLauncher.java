package org.brylex.maven.plugin;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.brylex.logging.Log4JLoggingSystem;
import org.sonatype.gshell.branding.Branding;
import org.sonatype.gshell.branding.BrandingSupport;
import org.sonatype.gshell.branding.License;
import org.sonatype.gshell.branding.LicenseSupport;
import org.sonatype.gshell.commands.jmx.MBeanCommand;
import org.sonatype.gshell.console.ConsoleErrorHandler;
import org.sonatype.gshell.console.ConsolePrompt;
import org.sonatype.gshell.guice.GuiceMainSupport;
import org.sonatype.gshell.launcher.Configuration;
import org.sonatype.gshell.logging.LoggingSystem;
import org.sonatype.gshell.shell.Shell;
import org.sonatype.gshell.shell.ShellErrorHandler;
import org.sonatype.gshell.shell.ShellPrompt;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmbeddedGShellLauncher extends GuiceMainSupport
{
    private final URL webappRoot;

    private final String groupId;
    private final String artifactId;
    private final String version;

    private final String applicationName;
    private final String mbeanName;

    public EmbeddedGShellLauncher(final URL webRoot)
    {
        this.webappRoot = webRoot;
        Properties properties = new Properties();

        InputStream is = EmbeddedGShellLauncher.class.getResourceAsStream("/bootstrap.properties");
        try
        {
            properties.load(is);
        } catch (IOException e)
        {
            throw new RuntimeException("Unable to load bootstrap.properties.", e);
        } finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                } catch (IOException e)
                {
                }
            }
        }

        this.groupId = properties.getProperty("project.groupId");
        this.artifactId = properties.getProperty("project.artifactId");
        this.version = properties.getProperty("project.version");

        this.applicationName = properties.getProperty("application.name");
        this.mbeanName = properties.getProperty("application.mbean");
    }

    public static void main(String[] args) throws Exception
    {
        Configuration configuration = new EmbeddedConfiguration();
        configuration.configure();

        new EmbeddedGShellLauncher(new URL(args[0])).boot(new String[]{});
    }

    @Override
    protected Shell createShell() throws Exception
    {
        Shell shell = super.createShell();

        if (mbeanName != null && mbeanName.trim().length() > 0) {
            shell.getVariables().set(MBeanCommand.JMX_MBEAN, mbeanName);
        } else {
            shell.getVariables().set(MBeanCommand.JMX_MBEAN, "<NO MBEAN>");
        }
        return shell;
    }

    @Override
    protected Branding createBranding()
    {
        return new MyBranding();
    }

    @Override
    protected void configure(List<Module> modules)
    {
        super.configure(modules);

        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(new Runnable()
        {
            public void run()
            {
                EmbeddedJettyServer server = new EmbeddedJettyServer();
                server.start(webappRoot);
            }
        });

        Module custom = new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(LoggingSystem.class).to(Log4JLoggingSystem.class);
                bind(ConsolePrompt.class).to(ShellPrompt.class);
                bind(ConsoleErrorHandler.class).to(ShellErrorHandler.class);

                bind(String.class).annotatedWith(Names.named("JMX_DEFAULT_MBEAN")).toInstance(mbeanName);
            }
        };

        modules.add(custom);
    }

    private class MyBranding extends BrandingSupport
    {
        @Override
        public String getDisplayName()
        {
            return applicationName;
        }

        @Override
        public String getWelcomeMessage()
        {
            return "#####################################################\n"
                    + "#    Group ID: " + groupId + "\n"
                    + "# Artifact ID: " + artifactId + "\n"
                    + "#     Version: " + version + "\n"
                    + "#####################################################\n";
        }

        @Override
        public String getPrompt()
        {
            return String.format("@|bold %s|@(${%s})> ", getProgramName(), MBeanCommand.JMX_MBEAN);
        }

        @Override
        public License getLicense()
        {
            return new LicenseSupport("Eclipse Public License, 1.0", getClass().getResource("license.txt"));
        }

    }
}
