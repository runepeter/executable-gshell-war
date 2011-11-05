package org.brylex.maven.plugin;

public class ExecutableWarMain extends AbstractMain {

    public ExecutableWarMain(final String bootstrapClassName) {
        super(bootstrapClassName);
    }

    public static void main(String[] args) {
        new ExecutableWarMain("org.brylex.maven.plugin.EmbeddedJettyServer");
    }

}
