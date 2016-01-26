/*
* Copyright 2014 Red Hat, Inc. and/or its affiliates.
*
* Licensed under the Eclipse Public License version 1.0, available at
* http://www.eclipse.org/legal/epl-v10.html
*/

package org.jboss.windup.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.impl.addons.AddonRepositoryImpl;
import org.jboss.forge.furnace.manager.impl.AddonManagerImpl;
import org.jboss.forge.furnace.manager.maven.MavenContainer;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.request.AddonActionRequest;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.se.FurnaceFactory;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.Version;
import org.jboss.forge.furnace.versions.Versions;
import org.jboss.windup.exec.WindupProcessor;
import org.jboss.windup.exec.configuration.WindupConfiguration;
import org.jboss.windup.exec.configuration.options.ExcludeTagsOption;
import org.jboss.windup.exec.configuration.options.IncludeTagsOption;
import org.jboss.windup.exec.configuration.options.OverwriteOption;
import org.jboss.windup.exec.configuration.options.SourceOption;
import org.jboss.windup.exec.configuration.options.TargetOption;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.GraphContextFactory;
import org.jboss.windup.rules.apps.java.config.ScanPackagesOption;
import org.jboss.windup.rules.apps.java.config.SourceModeOption;
import org.jboss.windup.util.PathUtil;
import org.jboss.windup.util.ZipUtil;
import org.jboss.windup.util.exception.WindupException;

/**
 * @author <a href="mailto:samueltauil@gmail.com">Samuel Tauil</a>
 * @author <a href="mailto:zizka@seznam.cz">Ondrej Zizka</a>
 */
@Mojo(name = "windup", requiresDependencyResolution = ResolutionScope.COMPILE, aggregator = true)
@Execute(phase = LifecyclePhase.GENERATE_SOURCES)
public class WindupMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project.build.directory}")
    private String buildDirectory;

    /**
     * Location of the generated report files.
     */
    @Parameter(alias = "output", property = "output", defaultValue = "${project.build.directory}/windup-report", required = true)
    private String outputDirectory;

    /**
     * Location of the input file application.
     */
    @Parameter( defaultValue = "${project.build.sourceDirectory}", property = "input", required = true )
    private String inputDirectory;

    /**
     * Packages to be inspected by Windup.
     */
    @Parameter(property = "packages", required = true)
    private List<String> packages;

    @Parameter(alias = "offline", property = "offline", required = false)
    private Boolean offlineMode;

    @Parameter( property = "sourceMode", required = false, defaultValue = "true" )
    private Boolean sourceMode;

    @Parameter( property = "overwrite", required = false )
    private Boolean overwrite;

    @Parameter(property = "userIgnorePath", required = false)
    private String userIgnorePath;

    @Parameter(property = "userRulesDirectory", required = false)
    private String userRulesDirectory;

    @Parameter( property = "includeTags", required = false )
    private List<String> includeTags;

    @Parameter( property = "excludeTags", required = false )
    private List<String> excludeTags;

    @Parameter( property = "sourceTechnologies", required = false )
    private List<String> sources;

    @Parameter( property = "targetTechnologies", required = false)
    private List<String> targets;


    @Parameter( property = "windupVersion", required = true )
    private String windupVersion;

    @Parameter(property = "forgeVersion", required = true)
    private String forgeVersion;

    public static final String WINDUP_RULES_GROUP_ID = "org.jboss.windup.rules";
    public static final String WINDUP_RULES_ARTIFACT_ID = "windup-rulesets";
    public static final String FORGE_ADDON_GROUP_ID = "org.jboss.forge.addon:";

    public void execute() throws MojoExecutionException
    {
        System.setProperty(PathUtil.WINDUP_HOME, Paths.get(buildDirectory, "winduphome").toString());

        WindupConfiguration windupConfiguration = new WindupConfiguration();
        windupConfiguration.setOptionValue(ScanPackagesOption.NAME, packages);
        windupConfiguration.addInputPath(Paths.get(inputDirectory));
        windupConfiguration.setOutputDirectory(Paths.get(outputDirectory));

        windupConfiguration.setOffline(offlineMode == Boolean.TRUE);
        windupConfiguration.setOptionValue(SourceModeOption.NAME, sourceMode == Boolean.TRUE);
        windupConfiguration.setOptionValue(OverwriteOption.NAME, overwrite == Boolean.TRUE);
        windupConfiguration.setOptionValue(IncludeTagsOption.NAME, includeTags);
        windupConfiguration.setOptionValue(ExcludeTagsOption.NAME, excludeTags);
        windupConfiguration.setOptionValue(SourceOption.NAME, sources);
        windupConfiguration.setOptionValue(TargetOption.NAME, targets);

        downloadAndUnzipRules();

        windupConfiguration.addDefaultUserRulesDirectory(PathUtil.getWindupRulesDir());

        if (userRulesDirectory != null && !Files.isDirectory(Paths.get(userRulesDirectory)))
        {
            try
            {
                Files.createDirectories(Paths.get(userRulesDirectory));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        if (userRulesDirectory != null)
            windupConfiguration.addDefaultUserRulesDirectory(Paths.get(userRulesDirectory));

        if (userIgnorePath == null)
        	userIgnorePath = PathUtil.getWindupIgnoreDir().toString();
        windupConfiguration.addDefaultUserIgnorePath(Paths.get(userIgnorePath));


        Furnace furnace = createAndStartFurnace();

        install("org.jboss.forge.addon:core,"+forgeVersion, true, furnace);
        //install("org.jboss.forge.addon:furnace,"+forgeVersion, true, furnace);
        //install("org.jboss.forge.furnace.container:cdi,"+forgeVersion, true, furnace);
        install("org.jboss.forge.furnace.container:simple,"+forgeVersion, true, furnace);
        install("org.jboss.forge.addon:convert,"+forgeVersion, true, furnace);
        //install("org.jboss.forge.addon:shell,"+forgeVersion, true, furnace);
        install("org.jboss.windup:windup-tooling,"+windupVersion, true, furnace);
        install("org.jboss.windup.exec:windup-exec,"+windupVersion, true, furnace);
        install("org.jboss.windup.utils:windup-utils,"+windupVersion, true, furnace);
        install("org.jboss.windup.ui:windup-ui,"+windupVersion, true, furnace);
        install("org.jboss.windup.rules.apps:windup-rules-java,"+windupVersion, true, furnace);
        install("org.jboss.windup.rules.apps:windup-rules-java,"+windupVersion, true, furnace);
        install("org.jboss.windup.rules.apps:windup-rules-java-ee,"+windupVersion, true, furnace);


        AddonRegistry addonRegistry = furnace.getAddonRegistry();
        WindupProcessor windupProcessor = addonRegistry.getServices(WindupProcessor.class).get();

        GraphContextFactory graphContextFactory = addonRegistry.getServices(GraphContextFactory.class).get();

        Path graphPath = windupConfiguration.getOutputDirectory().resolve("graph");
        try (GraphContext graphContext = graphContextFactory.create(graphPath))
        {
            windupConfiguration.setGraphContext(graphContext);
            windupProcessor.execute(windupConfiguration);
            System.out.println("Windup report created: " + windupConfiguration.getOutputDirectory().toAbsolutePath() + "/index.html");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadAndUnzipRules()
    {
        MavenContainer mavenContainer = new MavenContainer();
        RepositorySystem system = mavenContainer.getRepositorySystem();
        Settings settings = mavenContainer.getSettings();
        DefaultRepositorySystemSession session = mavenContainer.setupRepoSession(system, settings);
        ArtifactRequest artifactRequest = new ArtifactRequest();
        // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
        artifactRequest.setArtifact(new DefaultArtifact(WINDUP_RULES_GROUP_ID + ":" + WINDUP_RULES_ARTIFACT_ID + ":" + windupVersion));
        try
        {
            ArtifactResult artifactResult = mavenContainer.getRepositorySystem().resolveArtifact(session, artifactRequest);
            Path outputDirectory = PathUtil.getWindupRulesDir();
            if (!Files.isDirectory(Paths.get(userRulesDirectory)))
                Files.createDirectories(outputDirectory);
            ZipUtil.unzipToFolder(artifactResult.getArtifact().getFile(), outputDirectory.toFile());
        }
        catch (ArtifactResolutionException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static Furnace createAndStartFurnace()
    {
        // Create a Furnace instance. NOTE: This must be called only once
        Furnace furnace = FurnaceFactory.getInstance();
        // Add repository containing addons specified in pom.xml
        furnace.addRepository(AddonRepositoryMode.MUTABLE, new File("target/addons"));
        // Start Furnace in another thread
        System.setProperty("INTERACTIVE", "true");

        Future<Furnace> future = furnace.startAsync();
        try
        {
            // Wait until Furnace is started and return
            return future.get();
        }
        catch (InterruptedException | ExecutionException ex)
        {
            throw new WindupException("Failed to start Furnace: " + ex.getMessage(), ex);
        }
    }


    /**
     * TODO: Copied from Windup. Refactor.
     */
    private boolean install(String addonCoordinates, boolean batchMode, Furnace furnace)
    {
        Version runtimeAPIVersion = AddonRepositoryImpl.getRuntimeAPIVersion();
        try
        {
            AddonDependencyResolver resolver = new MavenAddonDependencyResolver();
            AddonManagerImpl addonManager = new AddonManagerImpl(furnace, resolver);

            AddonId addon = null;
            // This allows windup --install maven
            if (addonCoordinates.contains(","))
            {
                addon = AddonId.fromCoordinates((addonCoordinates.contains(":") ? "" : FORGE_ADDON_GROUP_ID) + addonCoordinates);
            }
            else
            {
                String coordinate = (addonCoordinates.contains(":") ? "" : FORGE_ADDON_GROUP_ID) + addonCoordinates;
                AddonId[] versions = resolver.resolveVersions(coordinate).get();

                if (versions.length == 0)
                    throw new IllegalArgumentException("No Artifact version found for " + coordinate);

                for (int i = versions.length - 1; i >= 0; i--)
                {
                    String apiVersion = resolver.resolveAPIVersion(versions[i]).get();
                    if (apiVersion != null && Versions.isApiCompatible(runtimeAPIVersion, SingleVersion.valueOf(apiVersion)))
                    {
                        addon = versions[i];
                        break;
                    }
                }
            }

            if (addon == null)
                throw new IllegalArgumentException("No compatible addon API version found for " + addonCoordinates + " for API " + runtimeAPIVersion);

            AddonActionRequest request = addonManager.install(addon);
            System.out.println(request);
            request.perform();
            System.out.println("Installation completed successfully.");
            System.out.println();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("> Forge version [" + runtimeAPIVersion + "]");
        }
        return true;
    }
}
