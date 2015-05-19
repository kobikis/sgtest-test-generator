package com.gigaspaces;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Kobi Kisos
 * @since 10.2.0
 * @see SGTestTestsGenerator
 * Generate SGTests tests metadata by scanning all test methods and extracting annotations from test classes.
 */
@SuppressWarnings({"UnusedDeclaration"})
@Mojo(name = "tests", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class SGTestTestGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}/sgtest-metadata.json")
    private File jsonTestFile;

    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    protected File sourcesDirectory;

    @Parameter(defaultValue = "${project.build.testOutputDirectory}")
    protected File testClassesDirectory;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    protected File classesDirectory;

    @Component
    protected MavenProject project;

    public void execute() throws MojoExecutionException {
        try {
            getLog().info("Generating TGRID metadata from source directory: " + sourcesDirectory);

            Classpath classpath = generateTestClasspath();
            ClassLoader cl = classpath.createClassLoader();

            List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
            classLoadersList.add(cl);

            Thread.currentThread().setContextClassLoader(cl);


            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setScanners(new SubTypesScanner(false), new MethodAnnotationsScanner(), new ResourcesScanner())
                    .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
                    .filterInputsBy(new FilterBuilder().includePackage("test")));

            Set<Method> methods =  reflections.getMethodsAnnotatedWith(Test.class);

            Class permGenClass =  cl.loadClass("com.gigaspaces.SGTestTestsGenerator");
            @SuppressWarnings("unchecked") Constructor constructor = permGenClass.getConstructor(Set.class);
            Object generator = constructor.newInstance(methods);

            scanPermutations(generator);
            writePermutationsToFile(generator);
        }
        catch (MojoExecutionException e) {
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException(e.toString(), e);
        }
    }

    private void writePermutationsToFile(Object permGen) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        permGen.getClass().getMethod("writeTestsToFile", File.class).invoke(permGen, jsonTestFile);
    }

    private void scanPermutations(Object permGen) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        permGen.getClass().getMethod("scanMethods").invoke(permGen);
    }


    private Classpath generateTestClasspath() throws MojoExecutionException {
        List<String> classpath = new ArrayList<String>(2 + getProject().getArtifacts().size());

        classpath.add(testClassesDirectory.getAbsolutePath());

        classpath.add(classesDirectory.getAbsolutePath());

        @SuppressWarnings("unchecked") Set<Artifact> classpathArtifacts = getProject().getArtifacts();

        for (Artifact artifact : classpathArtifacts) {
            if (artifact.getArtifactHandler().isAddedToClasspath()) {
                File file = artifact.getFile();
                if (file != null) {
                    classpath.add(file.getPath());
                }
            }
        }

        return new Classpath(classpath);
    }

    public MavenProject getProject() {
        return project;
    }

}
