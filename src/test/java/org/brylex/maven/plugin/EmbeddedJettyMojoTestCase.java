package org.brylex.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.project.MavenProject;

import java.io.File;

public class EmbeddedJettyMojoTestCase extends AbstractMojoTestCase
{
    private EmbeddedJettyMojo mojo;

    private static File pomFile = new File(getBasedir(), "target/test-classes/example-pom.xml");

    protected void setUp() throws Exception
    {
        super.setUp();

        this.mojo = (EmbeddedJettyMojo) lookupMojo("generate", pomFile);
    }

    public void testName() throws Exception
    {
        MavenProject project = new MavenProject();

        MavenSession mavenSession = new MavenSession( null, null, null, null, null, null, null, System.getProperties(), null );
        setVariableValueToObject( mojo, "session", mavenSession );
        mojo.setProject( project );

        ArtifactStub warArtifact = new ArtifactStub();

        project.setArtifact(warArtifact);

        mojo.execute();
    }
}
