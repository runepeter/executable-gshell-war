package org.brylex.maven.plugin;

public class ExecutableGShellWarMain extends AbstractMain {

    protected ExecutableGShellWarMain(final String bootstrapClassName) {
        super(bootstrapClassName);
    }

    public static void main(String[] args) {
        new ExecutableGShellWarMain("org.brylex.maven.plugin.EmbeddedJettyServer");
    }

}
