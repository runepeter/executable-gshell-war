package org.brylex.maven.plugin;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.webapp.WebAppContext;

public class EmbeddedJettyServer {
    public static void main(final String[] args) {
        Server server = new Server();

        Connector connector = new SocketConnector();
        connector.setPort(8080);
        server.setConnectors(new Connector[]{connector});

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(args[0]);
        server.setHandler(webapp);

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            throw new RuntimeException("Unable to start Jetty server.", e);
        } finally {
            server.destroy();
        }
    }
}
