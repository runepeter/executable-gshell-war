import org.brylex.maven.plugin.AbstractMain;
import org.brylex.maven.plugin.EmbeddedJettyServer;

public class ExecutableGShellWarMain extends AbstractMain {

    protected ExecutableGShellWarMain(Class<?> bootstrapClass) {
        super(bootstrapClass);
    }

    public static void main(String[] args) {
        new ExecutableGShellWarMain(EmbeddedJettyServer.class);
    }

}
