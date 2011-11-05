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
public class ExecutableGShellWarMojo extends AbstractExecutableWarMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("\n### Executable GShell/WAR Plugin ###\n\n");
        getLog().info("      Project: " + getProject());
        getLog().info("ProjectHelper: " + getProjectHelper());
    }
}
