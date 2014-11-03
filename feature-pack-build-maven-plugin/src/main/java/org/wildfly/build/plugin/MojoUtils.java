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
package org.wildfly.build.plugin;

import org.apache.maven.project.MavenProject;
import org.wildfly.build.Artifact;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.BuildArtifactResolver;
import org.wildfly.build.DelegatingArtifactResolver;
import org.wildfly.build.MapArtifactResolver;

import java.util.Map;
import java.util.Properties;

/**
 * @author Eduardo Martins
 */
public class MojoUtils {

    /**
     *
     * @param project
     * @return
     */
    public static Properties getBuildProperties(MavenProject project) {
        final Properties properties = new Properties();
        properties.putAll(project.getProperties());
        properties.putAll(System.getProperties());
        properties.put("project.version", project.getVersion()); //TODO: figure out the correct way to do this
        return properties;
    }

    /**
     *
     * @param systemPropertyVersionOverrides
     * @param properties
     * @param useMavenProjectArtifactResolver
     * @param project
     * @param artifactRefs
     * @return
     */
    public static BuildArtifactResolver createBuildArtifactResolver(boolean systemPropertyVersionOverrides, Properties properties, boolean useMavenProjectArtifactResolver, MavenProject project, Map<String, Artifact> artifactRefs) {
        ArtifactResolver artifactResolver = new MapArtifactResolver(artifactRefs);
        if (useMavenProjectArtifactResolver) {
            artifactResolver = new DelegatingArtifactResolver(new MavenProjectArtifactResolver(project), artifactResolver);
        }
        final Properties overrideProperties = systemPropertyVersionOverrides ? properties : null;
        return new BuildArtifactResolver(overrideProperties, artifactResolver);
    }
}
