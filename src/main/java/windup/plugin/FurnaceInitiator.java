package windup.plugin;

import org.apache.commons.io.FileUtils;
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
import org.jboss.windup.util.WindupPathUtil;
import org.jboss.windup.util.exception.WindupException;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by samueltauil on 12/19/14.
 */
public class FurnaceInitiator {


//    @Inject
//    public WindupProcessor windupProcessor;


    public GraphContextFactory getGraphContextFactory() {
        return graphContextFactory;
    }

    @Inject
    public GraphContextFactory graphContextFactory;

    public static final String FORGE_ADDON_GROUP_ID = "org.jboss.forge.addon:";

    public static void main(String[] args) throws IOException {
        Furnace furnace = FurnaceInitiator.getFurnace();
        FurnaceInitiator furnaceInitiator = new FurnaceInitiator();



        try {
//         furnace.setServerMode(true);
            furnaceInitiator.start(true, true, furnace);
//            furnace.setServerMode(true);
            System.setProperty("INTERACTIVE", "true");
//            System.setProperty("forge.shell.evaluate", "true");
//            org.jboss.windup:ui,2.0.0.Beta5
//            org.jboss.windup.rules.apps:rules-java,2.0.0.Beta5
//            org.jboss.windup.rules.apps:rules-java-ee,2.0.0.Beta5
//            furnaceInitiator.install("org.jboss.forge.addon:environment,2.12.1.Final", true, furnace);
//            furnaceInitiator.install("org.jboss.forge.addon:ui-spi,2.12.1.Final", true, furnace);
//            furnaceInitiator.install("org.codehaus.groovy:groovy-all,2.1.8", true, furnace);
//            furnaceInitiator.install("org.jboss.forge.furnace.container:cdi,2.12.1.Final", true, furnace);
//            furnaceInitiator.install("org.jboss.forge.addon:convert,2.12.1.Final", true, furnace);
//            furnaceInitiator.install("org.jboss.forge.addon:ui,2.12.1.Final", true, furnace);
//            furnaceInitiator.install("org.jboss.forge.furnace.container:simple,2.12.1.Final", true, furnace);
//            furnaceInitiator.install("org.jboss.forge.addon:environment,2.12.1.Final", true, furnace);
//            furnaceInitiator.install("org.jboss.forge.addon:facets,2.12.1.Final", true, furnace);
            furnaceInitiator.install("org.jboss.forge.addon:core,2.12.1.Final", true, furnace);
//            furnaceInitiator.install("org.jboss.forge.addon:shell,2.12.1.Final", true, furnace);

            furnaceInitiator.install("org.jboss.windup:ui,2.0.0.Beta5", true, furnace);
            furnaceInitiator.install("org.jboss.windup.rules.apps:rules-java,2.0.0.Beta5", true, furnace);
            furnaceInitiator.install("org.jboss.windup.rules.apps:rules-java-ee,2.0.0.Beta5", true, furnace);


//            furnaceInitiator.install("org.jboss.windup.config:windup-config,2.0.0.Beta6", true, furnace);
//            furnaceInitiator.install("org.jboss.windup.exec:windup-exec,2.0.0.Beta6", true, furnace);
//            furnaceInitiator.install("org.jboss.windup.utils:utils,2.0.0.Beta6", true, furnace);
//            furnaceInitiator.install("org.jboss.windup.graph:windup-graph,2.0.0.Beta6", true, furnace);
//            furnaceInitiator.install("org.jboss.windup.graph:windup-frames,2.0.0.Beta6", true, furnace);
//            furnaceInitiator.install("org.jboss.windup.graph:windup-graph-api,2.0.0.Beta6", true, furnace);
//            furnaceInitiator.install("org.jboss.windup:windup-bom,2.0.0.Beta5", true, furnace);
//            furnaceInitiator.install("org.jboss.windup.graph:windup-graph,2.0.0.Beta5", true, furnace);





            AddonRegistry addonRegistry = furnace.getAddonRegistry();
            WindupProcessor windupProcessor = addonRegistry.getServices(WindupProcessor.class).get();
            WindupConfiguration windupConfiguration = new WindupConfiguration();

            Path userRulesDir = WindupPathUtil.getWindupUserRulesDir();
            if (!Files.isDirectory(userRulesDir)) {
                Files.createDirectories(userRulesDir);
            }
            windupConfiguration.addDefaultUserRulesDirectory(userRulesDir);
            System.out.println(userRulesDir);
            Path userIgnoreDir = WindupPathUtil.getWindupIgnoreListDir();
            if (!Files.isDirectory(userIgnoreDir)) {
                Files.createDirectories(userIgnoreDir);
            }
            windupConfiguration.addDefaultUserIgnorePath(userIgnoreDir);

//            Path windupHomeRulesDir = WindupPathUtil.getWindupHomeRules();
//            if (!Files.isDirectory(windupHomeRulesDir)) {
//                Files.createDirectories(windupHomeRulesDir);
//            }
//            windupConfiguration.addDefaultUserRulesDirectory(windupHomeRulesDir);

//            Path windupHomeIgnoreDir = WindupPathUtil.getWindupHomeIgnoreListDir();
//            if (!Files.isDirectory(windupHomeIgnoreDir)) {
//                Files.createDirectories(windupHomeIgnoreDir);
//            }
//            windupConfiguration.addDefaultUserIgnorePath(windupHomeIgnoreDir);

            windupConfiguration.setOptionValue("packages", "org.apache");
            windupConfiguration.setOutputDirectory(Paths.get("/Users/samueltauil/Development/windup/windup-maven-plugin/target/"));
            FileUtils.deleteQuietly(windupConfiguration.getOutputDirectory().toFile());

            Path graphPath = windupConfiguration.getOutputDirectory().resolve("graph");
//            windupConfiguration.getGraphContext().getGraph();
            GraphContextFactory graphContextFactory = addonRegistry.getServices(GraphContextFactory.class).get();

            System.out.println(graphContextFactory);

            try (GraphContext graphContext = graphContextFactory.create(graphPath)) {

//                WindupProgressMonitor progressMonitor = new WindupProgressMonitorAdapter(uiProgressMonitor);
                windupConfiguration
//                        .setProgressMonitor(progressMonitor)
                        .setGraphContext(graphContext);
                windupProcessor.execute(windupConfiguration);

//                uiProgressMonitor.done();

                System.out.println(Results.success("Windup report created: " + windupConfiguration.getOutputDirectory().toAbsolutePath() + "/index.html"));
            }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }





    public FurnaceInitiator(){}

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
                                && Versions.isApiCompatible(runtimeAPIVersion, new SingleVersion(apiVersion)))
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
