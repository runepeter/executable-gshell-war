package org.brylex.maven.plugin;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.codehaus.plexus.archiver.UnArchiver;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * @author <a href="runepeter@gmail.com">Rune Peter Bjørnstad</a>
 * @version $Id$
 * @goal war
 * @phase package
 * @threadSafe
 * @requiresDependencyResolution runtime
 */
public class ExecutableWarMojo extends AbstractExecutableWarMojo {

    /**
     * Whether this is the main artifact being built. Set to <code>false</code> if you don't want to install or
     * deploy it to the local repository instead of the default one in an execution.
     *
     * @parameter expression="${primaryArtifact}" default-value="true"
     */
    private boolean primaryArtifact = true;

    public ExecutableWarMojo() {
        super(ExecutableWarMain.class);
    }

    protected ExecutableWarMojo(final Class<? extends AbstractMain> mainClass) {
        super(mainClass);
    }

    @Override
    protected void configureRuntimeDependencies() {
        groupId("org.eclipse.jetty").artifactId("jetty-webapp").version(resolveJettyVersion());
        groupId("org.slf4j").artifactId("slf4j-log4j12").version(resolveSlf4JVersion());
        groupId("log4j").artifactId("log4j").version(resolveLog4JVersion());
    }

    private String resolveJettyVersion() {
        return "8.0.4.v20111024"; // todo rpb: resolve from plugin pom (or manifest).
    }

    private String resolveSlf4JVersion() {
        return "1.6.2"; // todo rpb: resolve from plugin pom.
    }

    private String resolveLog4JVersion() {
        return "1.2.16"; // todo rpb: resolve from plugin pom.
    }

    @Override
    protected void installBootstrapResources() {

        File tmpDir = PluginUtils.mkdirs(getGenerateDir(), ".tmp/");

        extractPluginJar(tmpDir);

        try {
            Collection<File> files = FileUtils.listFiles(tmpDir, null, true);
            for (File file : files) {

                if (isClass(file, AbstractMain.class)) {
                    keepClass(file, tmpDir);
                }

                if (isClassloaderClassFile(file)) {
                    keepClass(file, tmpDir);
                }

                if (isClass(file, mainClass)) {
                    keepClass(file, tmpDir);
                }

                // todo rpb: may be a good idea to keep this file outside the WAR.
                if (file.getName().equals("log4j.properties")) {
                    FileUtils.moveFileToDirectory(file, getGenerateDir(), false);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy bootstrap resources.", e);
        } finally {
            try {
                FileUtils.deleteDirectory(tmpDir);
            } catch (IOException e) {
                throw new RuntimeException("Unable to delete tmp/ directory [" + tmpDir + "].", e);
            }
        }
    }

    private void keepClass(final File classFile, final File targetDir) throws IOException {
        String relativeFilename = ("" + classFile).substring(targetDir.toString().length());
        File packageDir = new File(getGenerateDir(), relativeFilename.substring(0, relativeFilename.lastIndexOf(relativeFilename.charAt(0)) + 1));

        getLog().info("Keeping bootstrap class [" + classFile.getName() + "].");
        FileUtils.moveFileToDirectory(classFile, packageDir, true);
    }

    private boolean isClass(final File file, Class<?> clazz) {
        getLog().info(file + " :: " + clazz.getCanonicalName());
        return file.getName().equals(clazz.getSimpleName() + ".class");
    }

    private boolean isClassloaderClassFile(final File file) {
        return file.getName().equals("AbstractMain$JarClassLoader.class");
    }

    private void extractPluginJar(File tmpDir) {

        DefaultArtifact pluginArtifact = DependencyBuilder
                .groupId("org.brylex.maven") // todo rpb: resolve from plugin pom.
                .artifactId("executable-war-plugin") // todo rpb: resolve from plugin pom.
                .version("0.1-SNAPSHOT") // todo rpb: resolve from plugin pom.
                .build();
        resolveDependency(pluginArtifact, tmpDir);

        UnArchiver unArchiver = getJarUnArchiver();
        unArchiver.setDestDirectory(tmpDir);
        unArchiver.setOverwrite(true);

        try {
            for (File file : tmpDir.listFiles(new JarFileFilter())) {
                unArchiver.setSourceFile(file);
                unArchiver.extract();
                FileUtils.moveFileToDirectory(file, getRuntimeLibDir(), false);
            }
            getLog().info("Extracted plugin artifact to dir [" + tmpDir + "].");
        } catch (Exception e) {
            throw new RuntimeException("Unable to extract plugin artifact.", e);
        }
    }

}
