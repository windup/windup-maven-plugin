package org.jboss.windup.plugin;


import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

public class WindupMojoTest extends AbstractMojoTestCase
{
    Properties versions = new Properties();

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        // required for mojo lookups to work
        super.setUp();


        try
        {
            final InputStream propsFile = WindupMojo.class.getClassLoader().getResourceAsStream("META-INF/versions.properties");
            versions.load(propsFile);
        }
        catch (IOException ex)
        {
            final String msg = "Can't load the version definitions from classpath: META-INF/versions.properties";
            throw new MojoExecutionException(msg, ex);
        }
    }

    public void testNoWindupVersionParameter() throws Exception
    {
        File testPom = new File( getBasedir(),
                "src/test/resources/mojoTestConfig.xml" );
        assertNotNull(testPom);

        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        MavenExecutionRequestPopulator populator = getContainer().lookup( MavenExecutionRequestPopulator.class );
        populator.populateDefaults(executionRequest);
        executionRequest.setSystemProperties(System.getProperties());

        ProjectBuildingRequest buildingRequest = executionRequest.getProjectBuildingRequest();
        ProjectBuilder projectBuilder = this.lookup(ProjectBuilder.class);
        MavenProject project = projectBuilder.build(testPom, buildingRequest).getProject();

        WindupMojo mojo = (WindupMojo) lookupConfiguredMojo(project, "windup");
        assertNotNull( mojo );
        assertNull(mojo.getWindupVersion());
        mojo.execute();


        assertEquals(mojo.getWindupVersion(), versions.getProperty("version.windup"));
        assertTrue("index.html report not found",
                Paths.get("",
                                ((Xpp3Dom) project.getBuild().getPlugins().get(0).getConfiguration()).getChild("outputDirectory").getValue(),
                                "index.html")
                        .toFile()
                        .exists());
    }

    public void testWindupVersionParameterPresent() throws Exception
    {
        File testPom = new File( getBasedir(),
                "src/test/resources/mojoTestConfigWithWindupVersion.xml" );
        assertNotNull(testPom);

        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        MavenExecutionRequestPopulator populator = getContainer().lookup( MavenExecutionRequestPopulator.class );
        populator.populateDefaults(executionRequest);
        executionRequest.setSystemProperties(System.getProperties());

        ProjectBuildingRequest buildingRequest = executionRequest.getProjectBuildingRequest();
        ProjectBuilder projectBuilder = this.lookup(ProjectBuilder.class);
        MavenProject project = projectBuilder.build(testPom, buildingRequest).getProject();

        WindupMojo mojo2 = (WindupMojo)lookupConfiguredMojo(project, "windup");
        assertNotNull( mojo2 );
        assertNotNull(mojo2.getWindupVersion());
        mojo2.execute();


        assertEquals(mojo2.getWindupVersion(), "6.2.0.Alpha2");
        assertTrue("index.html report not found",
                Paths.get("",
                                ((Xpp3Dom) project.getBuild().getPlugins().get(0).getConfiguration()).getChild("outputDirectory").getValue(),
                                "index.html")
                        .toFile()
                        .exists());
    }
}