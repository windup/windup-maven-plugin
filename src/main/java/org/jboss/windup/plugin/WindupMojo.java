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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.impl.addons.AddonRepositoryImpl;
import org.jboss.forge.furnace.manager.impl.AddonManagerImpl;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.request.AddonActionRequest;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.se.FurnaceFactory;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.Version;
import org.jboss.forge.furnace.versions.Versions;
import org.jboss.windup.exec.WindupProcessor;
import org.jboss.windup.exec.configuration.WindupConfiguration;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.GraphContextFactory;
import org.jboss.windup.util.PathUtil;
import org.jboss.windup.util.exception.WindupException;

/*
@author <a href="mailto:samueltauil@gmail.com">Samuel Tauil</a>
 */
@Mojo( name = "windup", requiresDependencyResolution = ResolutionScope.COMPILE, aggregator=true )
@Execute(phase=LifecyclePhase.GENERATE_SOURCES)
public class WindupMojo extends AbstractMojo {

    /**
     * Location of the generated report files.
     */
    @Parameter( defaultValue = "${project.build.directory}/windup-report", property = "output", required = true )
    private String outputDirectory;

    /**
     * Location of the input file application.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "input", required = true )
    private String inputDirectory;

    /**
     * Packages to be inspected by Windup.
     */
    @Parameter( property = "packages", required = true )
    private List<String> packages;

    @Parameter( property = "offline", required = false )
    private Boolean offlineMode;

    @Parameter( property = "overwrite", required = false )
    private Boolean overwrite;

    @Parameter( property = "userIgnorePath", required = false )
    private String userIgnorePath;

    @Parameter( property = "userRulesDirectory", required = false )
    private String userRulesDirectory;

    @Parameter( property = "windupVersion", required = true )
    private String windupVersion;

    @Parameter( property = "forgeVersion", required = true )
    private String forgeVersion;

    public static final String FORGE_ADDON_GROUP_ID = "org.jboss.forge.addon:";


    public void execute() throws MojoExecutionException {

        WindupConfiguration windupConfiguration = new WindupConfiguration();
        windupConfiguration.setOptionValue("packages", packages);
        // RM: Commented by upgrade to WindUp latest version
        //windupConfiguration.setInputPath(Paths.get(inputDirectory));
        windupConfiguration.addInputPath(Paths.get(inputDirectory));
        windupConfiguration.setOutputDirectory(Paths.get(outputDirectory));
        windupConfiguration.setOffline(offlineMode);
        windupConfiguration.setOptionValue("overwrite", overwrite);

        if  (userRulesDirectory == null) {
        	// RM: Modify by upgrade to WindUp latest version
        	// userRulesDirectory = WindupPathUtil.getWindupUserRulesDir().toString();
        	userRulesDirectory = PathUtil.getWindupRulesDir().toString();
        }

        if (userRulesDirectory != null && !Files.isDirectory(Paths.get(userRulesDirectory)))
        {
            try {
                Files.createDirectories(Paths.get(userRulesDirectory));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        windupConfiguration.addDefaultUserRulesDirectory(Paths.get(userRulesDirectory));


        if  (userIgnorePath == null) {
            // RM: Modify by upgrade to WindUp latest version
        	// userIgnorePath = WindupPathUtil.getWindupIgnoreListDir().toString();
        	userIgnorePath = PathUtil.getWindupIgnoreDir().toString();
        }

        if (userIgnorePath != null && !Files.isDirectory(Paths.get(userIgnorePath)))
        {
            try {
                Files.createDirectories(Paths.get(userIgnorePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        windupConfiguration.addDefaultUserIgnorePath(Paths.get(userIgnorePath));

        FileUtils.deleteQuietly(windupConfiguration.getOutputDirectory().toFile());
        Path graphPath = windupConfiguration.getOutputDirectory().resolve("graph");

        Furnace furnace = getFurnace();
        try {
            start(true, true, furnace);
            
            install("org.jboss.forge.addon:core,"+forgeVersion, true, furnace);
            install("org.jboss.windup.ui:windup-ui,"+windupVersion, true, furnace);
            install("org.jboss.windup.rules.apps:windup-rules-java,"+windupVersion, true, furnace);
            install("org.jboss.windup.rules.apps:windup-rules-java-ee,"+windupVersion, true, furnace);
//            install("org.jboss.forge.furnace.container:simple,"+forgeVersion, true, furnace);
            

            AddonRegistry addonRegistry = furnace.getAddonRegistry();
            WindupProcessor windupProcessor = addonRegistry.getServices(WindupProcessor.class).get();

            GraphContextFactory graphContextFactory = addonRegistry.getServices(GraphContextFactory.class).get();

            System.out.println(graphContextFactory);

            try (GraphContext graphContext = graphContextFactory.create(graphPath)) {

                windupConfiguration
                        .setGraphContext(graphContext);
                windupProcessor.execute(windupConfiguration);


                System.out.println(Results.success("Windup report created: " + windupConfiguration.getOutputDirectory().toAbsolutePath() + "/index.html"));

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }


    public static Furnace getFurnace() {


        // Create a Furnace instance. NOTE: This must be called only once
        Furnace furnace = FurnaceFactory.getInstance();
        // Add repository containing addons specified in pom.xml
        furnace.addRepository(AddonRepositoryMode.MUTABLE, new File("target/addons"));
        // Start Furnace in another thread
        System.setProperty("INTERACTIVE", "true");

        Future<Furnace> future = furnace.startAsync();
        try {
            // Wait until Furnace is started and return
            return future.get();
        }
        catch( InterruptedException | ExecutionException ex ) {
            throw new WindupException("Failed to start Furnace: " + ex.getMessage(), ex);
        }
    }

    public List<AddonId> getEnabledAddonIds(Furnace furnace)
    {
        List<AddonId> result = new ArrayList<>();
        for (AddonRepository repository : furnace.getRepositories())
        {
            List<AddonId> addons = repository.listEnabled();
            result.addAll(addons);
        }
        return result;
    }

    /**
     * Install core addons if none are installed; then start.
     */
    public void start(boolean exitAfter, boolean batchMode, Furnace furnace) throws InterruptedException, ExecutionException
    {
        if (exitAfter)
            return;

        if (!batchMode)
        {
            List<AddonId> addonIds = getEnabledAddonIds(furnace);
            if (addonIds.isEmpty())
            {
                String result = System.console().readLine(
                        "There are no addons installed; install core addons now? [Y,n] ");
                if (!"n".equalsIgnoreCase(result.trim()))
                {
                    install("core", batchMode, furnace);
                }
            }
        }

    }

    public boolean install(String addonCoordinates, boolean batchMode, Furnace furnace)
    {
        Version runtimeAPIVersion = AddonRepositoryImpl.getRuntimeAPIVersion();
        try
        {
            AddonDependencyResolver resolver = new MavenAddonDependencyResolver();
            AddonManagerImpl addonManager = new AddonManagerImpl(furnace, resolver);

            AddonId addon;
            // This allows windup --install maven
            if (addonCoordinates.contains(","))
            {
                if (addonCoordinates.contains(":"))
                {
                    addon = AddonId.fromCoordinates(addonCoordinates);
                }
                else
                {
                    addon = AddonId.fromCoordinates(FORGE_ADDON_GROUP_ID + addonCoordinates);
                }
            }
            else
            {
                AddonId[] versions;
                String coordinate;
                if (addonCoordinates.contains(":"))
                {
                    coordinate = addonCoordinates;
                    versions = resolver.resolveVersions(addonCoordinates).get();
                }
                else
                {
                    coordinate = FORGE_ADDON_GROUP_ID + addonCoordinates;
                    versions = resolver.resolveVersions(coordinate).get();
                }

                if (versions.length == 0)
                {
                    throw new IllegalArgumentException("No Artifact version found for " + coordinate);
                }
                else
                {
                    AddonId selected = null;
                    for (int i = versions.length - 1; selected == null && i >= 0; i--)
                    {
                        String apiVersion = resolver.resolveAPIVersion(versions[i]).get();
                        if (apiVersion != null
                                && Versions.isApiCompatible(runtimeAPIVersion, SingleVersion.valueOf(apiVersion)))
                        {
                            selected = versions[i];
                        }
                    }
                    if (selected == null)
                    {
                        throw new IllegalArgumentException("No compatible addon API version found for " + coordinate
                                + " for API " + runtimeAPIVersion);
                    }

                    addon = selected;
                }
            }

            AddonActionRequest request = addonManager.install(addon);
            System.out.println(request);
            if (!batchMode)
            {
                String result = System.console().readLine("Confirm installation [Y/n]? ");
                if ("n".equalsIgnoreCase(result.trim()))
                {
                    System.out.println("Installation aborted.");
                    return false;
                }
            }
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
