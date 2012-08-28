package org.brylex.maven.plugin;

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
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * @version $Id$
 */
public abstract class AbstractExecutableWarMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Local Maven repository where artifacts are cached during the build process.
     *
     * @parameter default-value="${localRepository}"
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter default-value="${project.remoteArtifactRepositories}"
     * @required
     */
    private List<ArtifactRepository> remoteRepositories;

    /**
     * To look up Archiver/UnArchiver implementations.
     *
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    private ArchiverManager archiverManager;

    /**
     * @component
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * @parameter default-value="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * @parameter default-value="${project.build.directory}/executable-war/"
     * @required
     * @readonly
     */
    private File generateDir;

    /**
     * @parameter default-value="${project.build.directory}/executable-war/WEB-INF/server/"
     * @required
     * @readonly
     */
    private File runtimeLibDir;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    private ArtifactResolver resolver;

    private List<ArtifactRepository> repositories;
    private Set<DependencyBuilder> dependencyBuilderSet;

    protected final Class<? extends AbstractMain> mainClass;

    public AbstractExecutableWarMojo(final Class<? extends AbstractMain> mainClass) {
        this.mainClass = mainClass;
    }

    protected abstract void configureRuntimeDependencies();

    protected abstract void installBootstrapResources();

    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("\n### Executable WAR Plugin ###\n\n");
        getLog().info("      Project: " + getProject());
        getLog().info("ProjectHelper: " + getProjectHelper());

        resolveRuntimeDependencies();
        installBootstrapResources();
        extractWar();
        createArtifact();
    }

    private void resolveRuntimeDependencies() {
        initRepositories();
        configureRuntimeDependencies();

        try {
            ArtifactRepository repository = createFlatRepository(getRuntimeLibDir());
            resolver.resolveTransitively(getDependencies(), getProject().getArtifact(), repositories, repository, new MavenMetadataSource());

            cleanNonJarFiles(getRuntimeLibDir());
            getLog().info("Runtime dependencies resolved to [" + getRuntimeLibDir() + "].");

        } catch (Exception e) {
            throw new RuntimeException("Unable to resolve runtime dependencies.", e);
        }
    }

    private void cleanNonJarFiles(File artifactDir) {
        for (File file : artifactDir.listFiles()) {
            if (!file.getName().endsWith(".jar")) {
                file.delete();
            }
        }
    }

    protected void initRepositories() {
        this.repositories = new ArrayList<ArtifactRepository>();
        repositories.add(getLocalRepository());
        repositories.addAll(getRemoteRepositories());
    }

    private Set<Artifact> getDependencies() {
        Set<Artifact> set = new HashSet<Artifact>(dependencyBuilderSet.size());
        for (DependencyBuilder builder : dependencyBuilderSet) {
            set.add(builder.build());
        }
        return set;
    }

    protected DependencyBuilder groupId(final String groupId) {

        if (dependencyBuilderSet == null) {
            this.dependencyBuilderSet = new HashSet<DependencyBuilder>();
        }

        DependencyBuilder builder = new DependencyBuilder(groupId);
        dependencyBuilderSet.add(builder);

        return builder;
    }

    protected void resolveDependency(final Artifact artifact, final File targetDir) {
        ArtifactRepository repository = createFlatRepository(targetDir);

        try {
            resolver.resolveAlways(artifact, asList(getLocalRepository()), repository);
        } catch (Exception e) {
            throw new RuntimeException("Unable to resolve artifact [" + artifact + "] to directory [" + targetDir + "].", e);
        }
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(final MavenProject project) {
        getLog().info("SETTER: " + project);
        this.project = project;
    }

    public MavenProjectHelper getProjectHelper() {
        return projectHelper;
    }

    public void setProjectHelper(final MavenProjectHelper projectHelper) {
        getLog().info("SETTER: " + projectHelper);
        this.projectHelper = projectHelper;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public void setLocalRepository(final ArtifactRepository localRepository) {
        getLog().info("SETTER: " + localRepository);
        this.localRepository = localRepository;
    }

    public List<ArtifactRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    public void setRemoteRepositories(final List<ArtifactRepository> remoteRepositories) {
        getLog().info("SETTER: " + remoteRepositories);
        this.remoteRepositories = remoteRepositories;
    }

    public ArchiverManager getArchiverManager() {
        return archiverManager;
    }

    public void setArchiverManager(ArchiverManager archiverManager) {
        getLog().info("SETTER: " + archiverManager);
        this.archiverManager = archiverManager;
    }

    protected UnArchiver getJarUnArchiver() {
        try {
            return archiverManager.getUnArchiver("jar");
        } catch (NoSuchArchiverException e) {
            throw new IllegalStateException("Unarchiver for type 'jar' is not available.");
        }
    }

    protected JarArchiver getJarArchiver() {
        try {
            return (JarArchiver) archiverManager.getArchiver("jar");
        } catch (NoSuchArchiverException e) {
            throw new IllegalStateException("Archiver for type 'jar' is not available.");
        }
    }

    public ArtifactRepositoryFactory getRepositoryFactory() {
        return repositoryFactory;
    }

    public void setRepositoryFactory(ArtifactRepositoryFactory repositoryFactory) {
        getLog().info("SETTER: " + repositoryFactory);
        this.repositoryFactory = repositoryFactory;
    }

    protected ArtifactRepository createFlatRepository(final File artifactDir) {
        return repositoryFactory.createDeploymentArtifactRepository(
                "local",
                toUrlString(artifactDir),
                new FlatLayout(),
                true);
    }

    private String toUrlString(File file) {
        try {
            return file.toURI().toURL().toExternalForm();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to create URL from file '" + file + "'.", e);
        }
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        getLog().info("SETTER: " + outputDirectory);
        this.outputDirectory = outputDirectory;
    }

    public File getGenerateDir() {
        return generateDir;
    }

    public void setGenerateDir(final File generateDir) {
        this.generateDir = generateDir;
    }

    public File getRuntimeLibDir() {
        return runtimeLibDir;
    }

    public void setRuntimeLibDir(final File runtimeLibDir) {
        this.runtimeLibDir = runtimeLibDir;
    }

    public ArtifactResolver getResolver() {
        return resolver;
    }

    public void setResolver(final ArtifactResolver resolver) {
        this.resolver = resolver;
    }

    // todo rpb: make an option that replaces original WAR file.
    private void createArtifact() {

        String filename = String.format("target/%s-%s-standalone.war", getProject().getArtifactId(), getProject().getVersion());
        File warFile = new File(getProject().getBasedir(), filename);

        try {
            Manifest manifest = new Manifest();
            manifest.addConfiguredAttribute(new Manifest.Attribute("Main-Class", mainClass.getName()));
            manifest.addConfiguredAttribute(new Manifest.Attribute("Class-Path", "./etc/"));
            JarArchiver archiver = getJarArchiver();
            archiver.addConfiguredManifest(manifest);
            archiver.setDestFile(warFile);
            archiver.addDirectory(getGenerateDir());
            archiver.createArchive();
            getLog().info("Executable WAR artifact [" + filename + "] successfully created.");

            getProject().getArtifact().setFile(warFile);

        } catch (Exception e) {
            throw new RuntimeException("Unable to generate artifact [" + filename + "].", e);
        }
    }

    private void extractWar() {
        File artifactFile = getProject().getArtifact().getFile();
        UnArchiver unArchiver = getJarUnArchiver();
        unArchiver.setDestDirectory(getGenerateDir());
        unArchiver.setSourceFile(artifactFile);
        try {
            unArchiver.extract();
            getLog().info("WAR artifact [" + artifactFile + "] extracted to [" + getGenerateDir() + "].");
        } catch (ArchiverException e) {
            throw new RuntimeException("Unable to extract WAR artifact.", e);
        }
    }

    protected static class FlatLayout extends DefaultRepositoryLayout {
        @Override
        public String pathOf(Artifact artifact) {
            String path = super.pathOf(artifact);

            return new File(path).getName();
        }
    }

    static class DependencyBuilder {

        private final String groupId;

        private String artifactId;
        private String version;
        private Set<String> excludes;

        public DependencyBuilder(final String groupId) {
            this.groupId = groupId;
            this.excludes = new HashSet<String>();
        }

        public static DependencyBuilder groupId(final String groupId) {
            return new DependencyBuilder(groupId);
        }

        DependencyBuilder artifactId(final String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        DependencyBuilder version(final String version) {
            this.version = version;
            return this;
        }

        DependencyBuilder exclude(final String excludeExpression) {
            excludes.add(excludeExpression);
            return this;
        }

        DefaultArtifact build() {
            DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, VersionRange.createFromVersion(version), "", "jar", null, new DefaultArtifactHandler(), false);
            artifact.setDependencyFilter(new ExclusionSetFilter(excludes));
            return artifact;
        }

    }
}
