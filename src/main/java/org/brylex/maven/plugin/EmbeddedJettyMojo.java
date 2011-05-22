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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * The name of the application (display name).
     *
     * @parameter default-value="${project.artifactId}"
     * @required
     * @readonly
     */
    private String applicationName;

    /**
     * The JMX MBean to connect to by default.
     *
     * @parameter
     * @readonly
     */
    private String mbean = null;

    /**
     * To look up Archiver/UnArchiver implementations.
     *
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    private ArchiverManager archiverManager;

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
     * @parameter default-value="${project.build.directory}"
     * @required
     * @readonly
     */
    private File targetDir;

    /**
     * @parameter default-value="${project.build.directory}/gshell-war-generate/"
     * @required
     * @readonly
     */
    private File generateDir;

    /**
     * @parameter default-value="${project.build.directory}/gshell-war-generate/WEB-INF/server/"
     * @required
     * @readonly
     */
    private File libDir;

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

    private List repositories;

    private Set<DependencyBuilder> dependencyBuilderSet;

    private void initRepositories()
    {
        this.repositories = new ArrayList();
        repositories.add(localRepository);
        repositories.addAll(remoteRepositories);
    }

    private void configureDependencies()
    {
        groupId("org.eclipse.jetty").artifactId("jetty-webapp").version("8.0.0.M2");
        groupId("org.eclipse.jetty").artifactId("jetty-jmx").version("8.0.0.M2");
        groupId("org.slf4j").artifactId("slf4j-log4j12").version("1.6.1");
        groupId("log4j").artifactId("log4j").version("1.2.16");
        groupId("org.sonatype.gshell").artifactId("gshell-core").version("2.6.5.1-SNAPSHOT");
        groupId("org.sonatype.gshell.ext").artifactId("gshell-gossip").version("2.6.5-SNAPSHOT").exclude("org.sonatype.gossip:gossip-slf4j");
        groupId("org.sonatype.gshell").artifactId("gshell-launcher").version("2.6.5-SNAPSHOT");
        groupId("org.sonatype.gshell.commands").artifactId("gshell-jmx").version("2.6.5.1-SNAPSHOT");
        groupId("org.sonatype.gshell.commands").artifactId("gshell-standard").version("2.6.5-SNAPSHOT");
    }

    private Set<Artifact> getDependencies()
    {
        Set<Artifact> set = new HashSet<Artifact>(dependencyBuilderSet.size());
        for (DependencyBuilder builder : dependencyBuilderSet)
        {
            set.add(builder.build());
        }
        return set;
    }

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        resolveRuntimeDependencies(libDir);
        installBootstrapResources();
        extractWar();
        createArtifact();
    }

    private void createArtifact()
    {
        String filename = String.format("target/%s-%s-standalone.war", project.getArtifactId(), project.getVersion());

        try
        {
            Manifest manifest = new Manifest();
            manifest.addConfiguredAttribute(new Manifest.Attribute("Main-Class", "Main"));
            JarArchiver archiver = getJarArchiver();
            archiver.addConfiguredManifest(manifest);
            archiver.setDestFile(new File(filename));
            archiver.addDirectory(generateDir);
            archiver.createArchive();
            getLog().info("Executable WAR artifact '" + filename + "' successfully created.");
        } catch (Exception e)
        {
            throw new RuntimeException("Unable to generate artifact '" + filename + "'.", e);
        }
    }

    private void extractWar()
    {
        File artifactFile = project.getArtifact().getFile();
        UnArchiver unArchiver = getJarUnArchiver();
        unArchiver.setDestDirectory(generateDir);
        unArchiver.setSourceFile(artifactFile);
        try
        {
            unArchiver.extract();
            getLog().info("WAR artifact '" + artifactFile + "' extracted to '" + generateDir + "'.");
        } catch (ArchiverException e)
        {
            throw new RuntimeException("Unable to extract WAR artifact.", e);
        }
    }

    private void installBootstrapResources()
    {
        DefaultArtifact pluginArtifact = DependencyBuilder.groupId("org.brylex.maven").artifactId("embedded-jetty-plugin").version("0.1-SNAPSHOT").build();
        File tmpDir = mkdirs(targetDir, "gshell-war-tmp/");
        resolveDependency(pluginArtifact, tmpDir);

        UnArchiver unArchiver = getJarUnArchiver();
        unArchiver.setDestDirectory(tmpDir);
        unArchiver.setOverwrite(true);

        try
        {
            for (File file : tmpDir.listFiles())
            {
                if (file.getName().endsWith(".jar"))
                {
                    unArchiver.setSourceFile(file);
                    unArchiver.extract();
                    FileUtils.moveFileToDirectory(file, libDir, false);
                }
            }
        } catch (Exception e)
        {
            throw new RuntimeException("Unable to extract plugin artifact.", e);
        }

        try
        {
            for (File file : tmpDir.listFiles())
            {
                if (file.getName().startsWith("Main"))
                {
                    FileUtils.moveFileToDirectory(file, generateDir, false);
                }

                if (file.getName().equals("log4j.properties"))
                {
                    FileUtils.moveFileToDirectory(file, generateDir, false);
                }
            }
        } catch (IOException e)
        {
            throw new RuntimeException("Unable to copy bootstrap resources.", e);
        }

        Bootstrap bootstrap = new Bootstrap(EmbeddedJettyServer.class, project.getGroupId(), project.getArtifactId(), project.getVersion());
        bootstrap.setApplicationName(applicationName);
        bootstrap.setMbeanName(mbean);
        bootstrap.toDir(generateDir);
        getLog().info("Bootstrap resources installed.");
    }

    private File mkdirs(final String path)
    {
        File targetDir = new File(path);
        targetDir.mkdirs();
        return targetDir;
    }

    private File mkdirs(final File parentDir, final String path)
    {
        File targetDir = new File(parentDir, path);
        targetDir.mkdirs();
        return targetDir;
    }

    private void resolveDependency(Artifact artifact, File targetDir)
    {
        ArtifactRepository repository = createFlatRepository(targetDir);

        try
        {
            resolver.resolve(artifact, asList(localRepository), repository);
        } catch (Exception e)
        {
            throw new RuntimeException("Unable to resolve artifact '" + artifact + "' to directory '" + targetDir + "'.", e);
        }
    }

    private void resolveRuntimeDependencies(File artifactDir)
    {
        configureDependencies();
        initRepositories();
        
        try
        {
            ArtifactRepository repository = createFlatRepository(artifactDir);
            resolver.resolveTransitively(getDependencies(), project.getArtifact(), repositories, repository, new MavenMetadataSource());

            cleanNonJarFiles(artifactDir);
            getLog().info("Runtime dependencies resolved to '" + artifactDir + "'.");

        } catch (Exception e)
        {
            throw new RuntimeException("Unable to resolve dependencies.", e);
        }
    }

    private void cleanNonJarFiles(File artifactDir)
    {
        for (File file : artifactDir.listFiles())
        {
            if (!file.getName().endsWith(".jar"))
            {
                file.delete();
            }
        }
    }

    private ArtifactRepository createFlatRepository(File artifactDir)
    {
        return repositoryFactory.createDeploymentArtifactRepository(
                "local",
                toUrlString(artifactDir),
                new FlatLayout(),
                true);
    }

    private String toUrlString(File file)
    {
        try
        {
            return file.toURI().toURL().toExternalForm();
        } catch (MalformedURLException e)
        {
            throw new RuntimeException("Unable to create URL from file '" + file + "'.", e);
        }
    }

    private UnArchiver getJarUnArchiver()
    {
        try
        {
            return archiverManager.getUnArchiver("jar");
        } catch (NoSuchArchiverException e)
        {
            throw new IllegalStateException("Unarchiver for type 'jar' is not available.");
        }
    }

    private JarArchiver getJarArchiver()
    {
        try
        {
            return (JarArchiver) archiverManager.getArchiver("jar");
        } catch (NoSuchArchiverException e)
        {
            throw new IllegalStateException("Archiver for type 'jar' is not available.");
        }
    }

    private DependencyBuilder groupId(final String groupId)
    {

        if (dependencyBuilderSet == null)
        {
            this.dependencyBuilderSet = new HashSet<DependencyBuilder>();
        }

        DependencyBuilder builder = new DependencyBuilder(groupId);
        dependencyBuilderSet.add(builder);

        return builder;
    }

    static class DependencyBuilder
    {

        private final String groupId;

        private String artifactId;
        private String version;
        private Set<String> excludes;

        DependencyBuilder(final String groupId)
        {
            this.groupId = groupId;
            this.excludes = new HashSet<String>();
        }

        public static DependencyBuilder groupId(final String groupId)
        {
            return new DependencyBuilder(groupId);
        }

        DependencyBuilder artifactId(final String artifactId)
        {
            this.artifactId = artifactId;
            return this;
        }

        DependencyBuilder version(final String version)
        {
            this.version = version;
            return this;
        }

        DependencyBuilder exclude(final String excludeExpression)
        {
            excludes.add(excludeExpression);
            return this;
        }

        DefaultArtifact build()
        {
            DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, VersionRange.createFromVersion(version), "", "jar", null, new DefaultArtifactHandler(), false);
            artifact.setDependencyFilter(new ExclusionSetFilter(excludes));
            return artifact;
        }

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
