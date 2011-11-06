package org.brylex.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @author <a href="runepeter@gmail.com">Rune Peter Bjørnstad</a>
 * @version $Id$
 * @goal gshell-war
 * @phase package
 * @threadSafe
 * @requiresDependencyResolution runtime
 */
public class ExecutableGShellWarMojo extends ExecutableWarMojo {

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
        bootstrap.setApplicationName("testing");
        bootstrap.toDir(getGenerateDir());
    }

    private String resolveJettyVersion() {
        return "8.0.4.v20111024"; // todo rpb: resolve from plugin pom (or manifest).
    }

    private String resolveGShellVersion() {
        return "2.6.5-SNAPSHOT";
    }

    private String resolveGShellExtVersion() {
        return "2.6.5.1-SNAPSHOT";
    }

}
