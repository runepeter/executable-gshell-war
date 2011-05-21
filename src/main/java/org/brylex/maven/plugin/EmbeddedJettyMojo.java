package org.brylex.maven.plugin;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * @requiresDependencyResolution runtime
 * @goal generate
 * @execute phase="package"
 * @threadSafe
 */
public class EmbeddedJettyMojo extends AbstractMojo
{
    /**
     * The Maven project.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The JAR archiver needed for archiving the classes directory into a JAR file under WEB-INF/lib.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" role-hint="jar"
     * @required
     */
    private JarArchiver jarArchiver;

    /**
     * To look up Archiver/UnArchiver implementations.
     *
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    private ArchiverManager archiverManager;

    /**
     * @parameter default-value="${session}"
     * @readonly
     * @required
     */
    private MavenSession session;

    /**
     * Local Maven repository where artifacts are cached during the build process.
     *
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter default-value="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    private List<ArtifactRepository> remoteRepositories;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    private ArtifactResolver resolver;

    /**
     * @component
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * @component role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    protected org.apache.maven.artifact.factory.ArtifactFactory factory;

    /**
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File classesDir;

    /**
     * Path to the default MANIFEST file to use. It will be used if
     * <code>useDefaultManifestFile</code> is set to <code>true</code>.
     *
     * @parameter expression="target/gen-tmp/META-INF/MANIFEST.MF"
     * @required
     * @readonly
     */
    private File defaultManifestFile;

    public void execute() throws MojoExecutionException, MojoFailureException
    {

        //unArchiver.setSourceFile(file);
        //unArchiver.setDestDirectory(location);

        System.err.println("Project: " + project);
        System.err.println("Local: " + localRepository);
        System.err.println("List: " + remoteRepositories);
        System.err.println("Resolver: " + resolver);

        DefaultArtifact artifact = new DefaultArtifact("org.eclipse.jetty", "jetty-webapp", VersionRange.createFromVersion("8.0.0.M2"), "", "jar", null, new DefaultArtifactHandler(), false);
        DefaultArtifact artifact1 = new DefaultArtifact("org.eclipse.jetty", "jetty-jmx", VersionRange.createFromVersion("8.0.0.M2"), "", "jar", null, new DefaultArtifactHandler(), false);
        DefaultArtifact artifact2 = new DefaultArtifact("org.slf4j", "slf4j-log4j12", VersionRange.createFromVersion("1.6.1"), "", "jar", null, new DefaultArtifactHandler(), false);
        DefaultArtifact artifact3 = new DefaultArtifact("log4j", "log4j", VersionRange.createFromVersion("1.2.16"), "", "jar", null, new DefaultArtifactHandler(), false);

        DefaultArtifact artifact4 = new DefaultArtifact("org.sonatype.gshell", "gshell-core", VersionRange.createFromVersion("2.6.5-SNAPSHOT"), "", "jar", null, new DefaultArtifactHandler(), false);
        DefaultArtifact artifact5 = new DefaultArtifact("org.sonatype.gshell.ext", "gshell-gossip", VersionRange.createFromVersion("2.6.5-SNAPSHOT"), "", "jar", null, new DefaultArtifactHandler(), false);
        artifact5.setDependencyFilter(new ExclusionSetFilter(new String[]{"org.sonatype.gossip:gossip-slf4j"}));
        DefaultArtifact artifact6 = new DefaultArtifact("org.sonatype.gshell", "gshell-launcher", VersionRange.createFromVersion("2.6.5-SNAPSHOT"), "", "jar", null, new DefaultArtifactHandler(), false);
        DefaultArtifact artifact7 = new DefaultArtifact("org.sonatype.gshell.commands", "gshell-jmx", VersionRange.createFromVersion("2.6.5-SNAPSHOT"), "", "jar", null, new DefaultArtifactHandler(), false);
        DefaultArtifact plugin = new DefaultArtifact("org.brylex.maven", "embedded-jetty-plugin", VersionRange.createFromVersion("0.1-SNAPSHOT"), "", "jar", null, new DefaultArtifactHandler(), false);

        try
        {
            JarArchiver archiver = (JarArchiver) archiverManager.getArchiver("jar");
            System.err.println("Archiver: " + archiver);

            UnArchiver unArchiver = archiverManager.getUnArchiver("jar");
            System.err.println("Unarchiver: " + unArchiver);

            File tmpLibDir = new File("target/tmp/lib");
            tmpLibDir.mkdirs();

            ArtifactRepository tmpLibRepository = repositoryFactory.createDeploymentArtifactRepository(
                    "local",
                    tmpLibDir.toURL().toExternalForm(),
                    new FlatLayout(),
                    true);

            File webInfServerDir = new File("target/gen-tmp/WEB-INF/server/");
            webInfServerDir.mkdirs();

            ArtifactRepository webInfServerRepository = repositoryFactory.createDeploymentArtifactRepository(
                    "local",
                    webInfServerDir.toURL().toExternalForm(),
                    new FlatLayout(),
                    true);

            HashSet set = new HashSet();
            set.add(artifact);
            set.add(artifact1);
            set.add(artifact2);
            set.add(artifact3);

            List repos = new ArrayList();
            repos.add(localRepository);
            repos.addAll(remoteRepositories);

            resolver.resolveTransitively(set, project.getArtifact(), repos, webInfServerRepository, new MavenMetadataSource());

            HashSet set2 = new HashSet();
            set2.add(artifact4);
            set2.add(artifact5);
            set2.add(artifact6);
            set2.add(artifact7);

            resolver.resolveTransitively(set2, project.getArtifact(), repos, webInfServerRepository, new MavenMetadataSource());

            for (File file : webInfServerDir.listFiles())
            {
                if (!file.getName().endsWith(".jar")) {
                    file.delete();
                }
            }

            resolver.resolve(plugin, asList(localRepository), tmpLibRepository);

            File targetDir = new File("target/gen-tmp/");
            targetDir.mkdirs();

            File tmpDir = new File("target/tmp/");
            tmpDir.mkdirs();

            unArchiver.setDestDirectory(tmpDir);
            unArchiver.setOverwrite(true);

            for (File f : tmpLibDir.listFiles())
            {
                if (f.getName().endsWith(".jar"))
                {
                    unArchiver.setSourceFile(f);
                    unArchiver.extract();
                    System.err.println(f.getName() + " extracted...");
                    FileUtils.moveFileToDirectory(f, webInfServerDir, false);
                }
            }

            for (File file : tmpDir.listFiles())
            {
                if (file.getName().startsWith("Bootstrap")) {
                    FileUtils.moveFileToDirectory(file, targetDir, false);
                    System.err.println(file + " copied.");
                }

                if (file.getName().equals("log4j.properties")) {
                    FileUtils.moveFileToDirectory(file, targetDir, false);
                    System.err.println(file + " copied.");
                }
            }

            File artifactFile = project.getArtifact().getFile();
            System.err.println("WAR: " + artifactFile);

            unArchiver.setDestDirectory(targetDir);
            unArchiver.setSourceFile(artifactFile);
            unArchiver.extract();
            System.err.println("Extracted WAR file " + artifactFile);



            //MavenArchiveConfiguration configuration = new MavenArchiveConfiguration();
            //configuration.addManifestEntry("Main-Class", getClass().getName());
            //archiver.setManifest(configuration.getManifestFile());

            //archiver.setManifest(defaultManifestFile);

            Manifest manifest = new Manifest();
            manifest.addConfiguredAttribute(new Manifest.Attribute("Main-Class", "Bootstrap"));
            archiver.addConfiguredManifest(manifest);

            archiver.setDestFile(new File("target/jalla.war"));
            archiver.addDirectory(targetDir);
            archiver.createArchive();
            System.err.println("Archive created...");

        } catch (Exception e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject(MavenProject project)
    {
        this.project = project;
    }

    private class FlatLayout extends DefaultRepositoryLayout
    {
        @Override
        public String pathOf(Artifact artifact)
        {
            String path = super.pathOf(artifact);

            return new File(path).getName();
        }
    }

}
