/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.build.provisioning;

import org.wildfly.build.AetherArtifactFileResolver;
import org.wildfly.build.BuildArtifactResolver;
import org.wildfly.build.BuildProperties;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;
import org.wildfly.build.provisioning.model.ServerProvisioningDescriptionModelParser;
import org.wildfly.build.MapArtifactResolver;
import org.wildfly.build.util.MapPropertyResolver;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * The standalone app to provision a WildFly server.
 *
 * @author Eduardo Martins
 */
public class StandaloneServerProvisioning {
    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        final File configFile = new File(args.length == 1 ? args[0] : "server-provisioning.xml");
        // environment is the sys properties
        final Properties environment = System.getProperties();
        // setup build dir
        final File buildDir = new File("target");
        // create the standalone aether artifact file resolver, reuse maven local repo if found at standard location
        final File mavenLocalRepositoryBaseDir = new File(new File(System.getProperty("user.home"), ".m2"), "repository");
        final AetherArtifactFileResolver aetherArtifactFileResolver = new StandaloneAetherArtifactFileResolver(mavenLocalRepositoryBaseDir.exists() ? mavenLocalRepositoryBaseDir : (new File(buildDir, "repository")));
        try (FileInputStream configStream = new FileInputStream(configFile)) {
            // parse description
            final ServerProvisioningDescription serverProvisioningDescription = new ServerProvisioningDescriptionModelParser(new MapPropertyResolver(environment)).parse(configStream);
            // create version override artifact resolver
            final boolean systemPropertyVersionOverrides = Boolean.valueOf(environment.getProperty(BuildProperties.SYSTEM_PROPERTIES_VERSION_OVERRIDES, "false"));
            final BuildArtifactResolver buildArtifactResolver = new BuildArtifactResolver((systemPropertyVersionOverrides ? environment : null), new MapArtifactResolver(serverProvisioningDescription.getArtifactRefs()));
            // provision the server
            final File outputDir = new File(buildDir, "wildfly");
            ServerProvisioner.build(serverProvisioningDescription, outputDir, aetherArtifactFileResolver, buildArtifactResolver);
            System.out.println("Server provisioning at " + outputDir + " complete.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
