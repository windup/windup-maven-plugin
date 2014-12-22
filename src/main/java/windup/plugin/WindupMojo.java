package windup.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.se.FurnaceFactory;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.windup.config.WindupConfigurationOption;
import org.jboss.windup.exec.WindupProcessor;
import org.jboss.windup.exec.configuration.WindupConfiguration;
import org.jboss.windup.exec.configuration.options.OverwriteOption;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.GraphContextFactory;
import org.jboss.windup.util.WindupPathUtil;
import org.jboss.windup.util.exception.WindupException;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Mojo( name = "windup", requiresDependencyResolution = ResolutionScope.COMPILE, aggregator=true)
@Execute(phase=LifecyclePhase.GENERATE_SOURCES)
public class WindupMojo extends AbstractMojo {

    /**
     * Location of the generated report files.
     */
    @Parameter( defaultValue = "${project.build.directory}/windup-report", property = "output", required = true )
    private File outputDirectory;

    /**
     * Location of the input file application.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "input", required = true )
    private File inputDirectory;

    @Parameter( property = "packages", required = true)
    private List<String> packages;

    @Inject
    private GraphContextFactory graphContextFactory;

    @Inject
    private WindupProcessor processor;

    public void execute() throws MojoExecutionException {

        WindupConfiguration windupConfiguration = new WindupConfiguration();
        windupConfiguration.setOptionValue("packages", packages);
        windupConfiguration.setOptionValue("input", inputDirectory);
        windupConfiguration.setOptionValue("output", outputDirectory);

        java.nio.file.Path userRulesDir = WindupPathUtil.getWindupUserRulesDir();
        if (userRulesDir != null && !Files.isDirectory(userRulesDir))
        {
            try {
                Files.createDirectories(userRulesDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        windupConfiguration.addDefaultUserRulesDirectory(userRulesDir);

        Path userIgnoreDir = WindupPathUtil.getWindupIgnoreListDir();
        if (userIgnoreDir != null && !Files.isDirectory(userIgnoreDir))
        {
            try {
                Files.createDirectories(userIgnoreDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        windupConfiguration.addDefaultUserIgnorePath(userIgnoreDir);

        Path windupHomeRulesDir = WindupPathUtil.getWindupHomeRules();
        if (windupHomeRulesDir != null && !Files.isDirectory(windupHomeRulesDir))
        {
            try {
                Files.createDirectories(windupHomeRulesDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        windupConfiguration.addDefaultUserRulesDirectory(windupHomeRulesDir);

        Path windupHomeIgnoreDir = WindupPathUtil.getWindupHomeIgnoreListDir();
        if (windupHomeIgnoreDir != null && !Files.isDirectory(windupHomeIgnoreDir))
        {
            try {
                Files.createDirectories(windupHomeIgnoreDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        windupConfiguration.addDefaultUserIgnorePath(windupHomeIgnoreDir);

        Boolean overwrite = (Boolean) windupConfiguration.getOptionMap().get(OverwriteOption.NAME);
        if (overwrite == null)
        {
            overwrite = false;
        }
        if (!overwrite && pathNotEmpty(windupConfiguration.getOutputDirectory().toFile()))
        {
//            String promptMsg = "Overwrite all contents of \"" + windupConfiguration.getOutputDirectory().toString()
//                    + "\" (anything already in the directory will be deleted)?";
//            if (!context.getPrompt().promptBoolean(promptMsg, false))
//            {
//                String outputPath = windupConfiguration.getOutputDirectory().toString();
//                return Results.fail("Files exist in " + outputPath + ", but --overwrite not specified. Aborting!");
//            }
        }

        // put this in the context for debugging, and unit tests (or anything else that needs it)
//        context.getUIContext().getAttributeMap().put(org.jboss.windup.exec.configuration.WindupConfiguration.class, windupConfiguration);

        FileUtils.deleteQuietly(windupConfiguration.getOutputDirectory().toFile());
        Path graphPath = windupConfiguration.getOutputDirectory().resolve("graph");

        Furnace furnace = getFurnace();


        try {
            try (GraphContext graphContext = graphContextFactory.create(graphPath))
            {
                windupConfiguration
                        .setGraphContext(graphContext);

                processor.execute(windupConfiguration);


                System.out.println(Results.success("Windup report created: "
                    + windupConfiguration.getOutputDirectory().toAbsolutePath() + "/index.html"));

        }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Furnace getFurnace() {

                 // Create a Furnace instance. NOTE: This must be called only once
                         Furnace furnace = FurnaceFactory.getInstance();
                         // Add repository containing addons specified in pom.xml
                         //furnace.addRepository(AddonRepositoryMode.IMMUTABLE, new File("target/addons"));
                                        // Start Furnace in another thread
                                                 Future<Furnace> future = furnace.startAsync();
         try {
             // Wait until Furnace is started and return
                     return future.get();
             }
         catch( InterruptedException | ExecutionException ex ) {
             throw new WindupException("Failed to start Furnace: " + ex.getMessage(), ex);
             }
    }

    public static File getUserWindupDir()
    {
        return new File(OperatingSystemUtils.getUserHomeDir(), ".windup").getAbsoluteFile();
    }



    private boolean pathNotEmpty(File f)
    {
        if (f.exists() && !f.isDirectory())
        {
            return true;
        }
        if (f.isDirectory() && f.listFiles() != null && f.listFiles().length > 0)
        {
            return true;
        }
        return false;
    }

    private class WindupOptionAndInput
    {
        private WindupConfigurationOption option;
        private InputComponent<?, ?> input;

        public WindupOptionAndInput(WindupConfigurationOption option, InputComponent<?, ?> input)
        {
            this.option = option;
            this.input = input;
        }
    }
}
