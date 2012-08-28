package org.brylex.maven.plugin;

/**
 * @author <a href="runepeter@gmail.com">Rune Peter Bjørnstad</a>
 * @version $Id$
 * @goal gshell-war
 * @phase package
 * @threadSafe
 * @requiresDependencyResolution runtime
 */
public class ExecutableGShellWarMojo extends ExecutableWarMojo {

    /**
     * The application identifier.
     *
     * @parameter default-value="${project.artifactId}"
     */
    private String applicationName;

    /**
     * The default mbean to start with.
     *
     * @parameter default-value="<NO MBEAN>"
     */
    private String mbean;

    /**
     * The Jetty HTTP server port
     *
     * @parameter default-value="8080"
     */
    private String port;

    public ExecutableGShellWarMojo() {
        super(ExecutableGShellWarMain.class);
    }

    @Override
    protected void configureRuntimeDependencies() {
        super.configureRuntimeDependencies();
        groupId("org.eclipse.jetty").artifactId("jetty-jmx").version(resolveJettyVersion());
        groupId("org.sonatype.gshell").artifactId("gshell-core").version(resolveGShellExtVersion());
        groupId("org.sonatype.gshell.ext").artifactId("gshell-gossip").version(resolveGShellVersion()).exclude("org.sonatype.gossip:gossip-slf4j");
        groupId("org.sonatype.gshell").artifactId("gshell-launcher").version(resolveGShellVersion());
        groupId("org.sonatype.gshell.commands").artifactId("gshell-jmx").version(resolveGShellExtVersion());
        groupId("org.sonatype.gshell.commands").artifactId("gshell-standard").version(resolveGShellVersion());
    }

    @Override
    protected void installBootstrapResources() {
        super.installBootstrapResources();
        Bootstrap bootstrap = new Bootstrap(getClass(), getProject().getGroupId(), getProject().getArtifactId(), getProject().getVersion());
        bootstrap.setApplicationName(applicationName);
        bootstrap.setApplicationMbean(mbean);
        bootstrap.setApplicationPort(port);
        bootstrap.toDir(getGenerateDir());
    }

    private String resolveJettyVersion() {
        return "8.0.4.v20111024"; // todo rpb: resolve from plugin pom (or manifest).
    }

    private String resolveGShellVersion() {
        return "2.6.5";
    }

    private String resolveGShellExtVersion() {
        return "2.6.5.1-SNAPSHOT";
    }

}
