package org.brylex.maven.plugin;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Set;

@Singleton
public class EmbeddedJettyServer
{
    @PostConstruct
    public void start(URL webappRoot)
    {
        Server server = new Server();

        Connector connector = new SocketConnector();
        connector.setPort(8080);
        server.setConnectors(new Connector[]{connector});

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(webappRoot.toExternalForm());
        server.setHandler(webapp);

        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanContainer mbeanContainer = new MBeanContainer(mbeanServer);
        server.getContainer().addEventListener(mbeanContainer);

        try
        {
            mbeanContainer.start();
            server.start();
            server.join();
        } catch (Exception e)
        {
            throw new RuntimeException("Unable to start Jetty server.", e);
        } finally
        {
            server.destroy();
        }

    }

}
