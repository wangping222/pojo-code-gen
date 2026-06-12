package io.github.youngerier.generator.maven;

import io.github.youngerier.generator.GeneratorConfig;
import io.github.youngerier.generator.GeneratorEngine;
import io.github.youngerier.generator.annotation.GenModel;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;

import static io.github.youngerier.generator.GeneratorConstants.SRC_MAIN_JAVA;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CodeGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * List of packages to scan for POJOs.
     */
    @Parameter(property = "pojo.codegen.scanPackages", required = true)
    private List<String> scanPackages;

    /**
     * Base directory where the generated Java files will be saved.
     * Note: The final output will be inside a 'src/main/java' subdirectory of this path.
     * Defaults to ${project.build.directory}/generated-sources/
     */
    @Parameter(property = "pojo.codegen.outputDir", defaultValue = "${project.build.directory}/generated-sources/")
    private File outputDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Starting POJO code generation...");

        if (scanPackages == null || scanPackages.isEmpty()) {
            getLog().warn("No packages to scan configured. Skipping code generation.");
            return;
        }

        ensureProjectCompiled();
        outputDir.mkdirs();

        List<Class<?>> pojoClasses = findPojoClasses();
        if (pojoClasses.isEmpty()) {
            getLog().warn("No POJOs with @GenModel annotation found. Skipping.");
            return;
        }

        GeneratorConfig config = GeneratorConfig.builder()
                .moduleName(project.getArtifactId())
                .outputBaseDir(outputDir.getAbsolutePath())
                .pojoClasses(pojoClasses)
                .build();

        new GeneratorEngine(config).execute();

        File generatedSourcesDir = new File(outputDir, SRC_MAIN_JAVA);
        project.addCompileSourceRoot(generatedSourcesDir.getAbsolutePath());
        getLog().info("Code generation completed. Sources: " + generatedSourcesDir);
    }

    private void ensureProjectCompiled() throws MojoExecutionException {
        File outputDirectory = new File(project.getBuild().getOutputDirectory());
        File[] files = outputDirectory.listFiles();
        if (outputDirectory.exists() && files != null && files.length > 0) {
            return;
        }

        getLog().info("Project classes not found, attempting to compile...");
        try {
            compileProject();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to compile project before code generation", e);
        }
    }

    private List<Class<?>> findPojoClasses() throws MojoExecutionException {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            List<URL> urls = buildClasspathUrls();
            URLClassLoader customClassLoader = new URLClassLoader(
                    urls.toArray(new URL[0]),
                    this.getClass().getClassLoader()
            );
            Thread.currentThread().setContextClassLoader(customClassLoader);

            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setUrls(urls)
                    .setScanners(Scanners.TypesAnnotated)
                    .forPackages(scanPackages.toArray(new String[0]))
                    .addClassLoaders(customClassLoader));

            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(GenModel.class);
            getLog().info("Found " + annotatedClasses.size() + " @GenModel classes: " + annotatedClasses);
            return List.copyOf(annotatedClasses);
        } catch (Exception e) {
            throw new MojoExecutionException("Error scanning for POJO classes", e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private List<URL> buildClasspathUrls() {
        List<String> classpathElements = getProjectClasspathElements();
        return classpathElements.stream()
                .map(File::new)
                .filter(File::exists)
                .map(f -> {
                    try {
                        return f.toURI().toURL();
                    } catch (Exception e) {
                        getLog().warn("Failed to convert to URL: " + f);
                        return null;
                    }
                })
                .filter(u -> u != null)
                .toList();
    }

    private List<String> getProjectClasspathElements() {
        try {
            return project.getCompileClasspathElements();
        } catch (Exception e) {
            getLog().warn("Failed to get classpath elements, using output directory only");
            return List.of(project.getBuild().getOutputDirectory());
        }
    }

    private void compileProject() throws Exception {
        getLog().info("Compiling project...");
        ProcessBuilder pb = new ProcessBuilder("mvn", "compile", "-q")
                .directory(project.getBasedir())
                .redirectErrorStream(true);

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            getLog().debug("Compilation output: " + output);
            throw new Exception("mvn compile failed with exit code: " + exitCode);
        }
        getLog().info("Project compiled successfully");
    }
}
