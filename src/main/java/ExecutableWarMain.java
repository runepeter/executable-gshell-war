import org.brylex.maven.plugin.AbstractMain;
import org.brylex.maven.plugin.EmbeddedJettyServer;

public class ExecutableWarMain extends AbstractMain {

    public ExecutableWarMain(Class<?> bootstrapClass) {
        super(bootstrapClass);
    }

    public static void main(String[] args) {
        new ExecutableWarMain(EmbeddedJettyServer.class);
    }

}
