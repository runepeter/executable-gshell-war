package org.brylex.maven.plugin;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.brylex.logging.Log4JLoggingSystem;
import org.sonatype.gshell.branding.Branding;
import org.sonatype.gshell.branding.BrandingSupport;
import org.sonatype.gshell.branding.License;
import org.sonatype.gshell.branding.LicenseSupport;
import org.sonatype.gshell.console.ConsoleErrorHandler;
import org.sonatype.gshell.console.ConsolePrompt;
import org.sonatype.gshell.guice.GuiceMainSupport;
import org.sonatype.gshell.launcher.Configuration;
import org.sonatype.gshell.logging.LoggingSystem;
import org.sonatype.gshell.shell.ShellErrorHandler;
import org.sonatype.gshell.shell.ShellPrompt;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmbeddedGShellLauncher extends GuiceMainSupport
{
    private final URL webappRoot;
    
    public EmbeddedGShellLauncher(final URL webappRoot)
    {
        this.webappRoot = webappRoot;
    }

    public static void main(String[] args) throws Exception
    {
        Configuration configuration = new EmbeddedConfiguration();
        configuration.configure();

        new EmbeddedGShellLauncher(new URL(args[0])).boot(new String[]{});
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
            }
        };

        modules.add(custom);
    }

    private class MyBranding extends BrandingSupport
    {
        @Override
        public String getDisplayName()
        {
            return "Jalla, Balla!";
        }

        @Override
        public String getWelcomeMessage()
        {
            return "Hei der din gris";
        }

        @Override
        public String getPrompt()
        {
            return String.format("@|bold %s|@(${%s}):${shell.home}> ", getProgramName(), "blipp");
        }

        @Override
        public File getUserContextDir()
        {
            return new File(getUserHomeDir(), ".m2");
        }

        @Override
        public License getLicense()
        {
            return new LicenseSupport("Eclipse Public License, 1.0", getClass().getResource("license.txt"));
        }

    }
}
