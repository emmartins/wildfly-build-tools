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

package org.wildfly.build;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * The artifact resolver in the 'feature pack' or 'server provisioning' build context.
 *
 * The build artifact resolver provides the description's artifact-refs resolving, and is also responsible for overriding other resolver's artifacts, such as the feature pack ones.
 *
 * Any artifact attribute may be fully overridden by redefining an artifact in its inner resolver, and on top of that, an optional map of properties may also be used to override artifact versions.
 *
 * @author Eduardo Martins
 */
public class BuildArtifactResolver implements OverrideArtifactResolver {

    public static final String OVERRIDE_PROPERTY_NAME_PREFIX = "version.";

    private final Map<String, String> overrideProperties = new HashMap<>();
    private final ArtifactResolver artifactResolver;

    public BuildArtifactResolver(Properties overrideProperties, ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
        // extract version override properties and override resolver's artifact versions
        if (overrideProperties != null) {
            for (String propertyName : overrideProperties.stringPropertyNames()) {
                if (propertyName.length() > OVERRIDE_PROPERTY_NAME_PREFIX.length() && propertyName.startsWith(OVERRIDE_PROPERTY_NAME_PREFIX)) {
                    // a version override property
                    final String artifactName = propertyName.substring(OVERRIDE_PROPERTY_NAME_PREFIX.length());
                    final String overrideVersion = overrideProperties.getProperty(propertyName);
                    this.overrideProperties.put(artifactName, overrideVersion);
                    // override resolver's artifact version, if present
                    final Artifact artifact = this.artifactResolver.getArtifact(artifactName);
                    if (artifact != null && !artifact.getVersion().equals(overrideVersion)) {
                        artifact.setVersion(overrideVersion);
                    }
                }
            }
        }
    }

    @Override
    public Map<String, Artifact> override(ArtifactResolver artifactResolver) {
        final Map<String, Artifact> overriddenArtifacts = new HashMap<>();
        for (String artifactName : artifactResolver.getArtifactNames()) {
            final Artifact artifact = artifactResolver.getArtifact(artifactName);
            final Artifact override = this.artifactResolver.getArtifact(artifactName);
            if (override != null) {
                artifact.setGroupId(override.getGroupId());
                artifact.setArtifactId(override.getArtifactId());
                artifact.setExtension(override.getExtension());
                artifact.setClassifier(override.getClassifier());
                artifact.setVersion(override.getVersion());
                overriddenArtifacts.put(artifactName, artifact);
            } else {
                final String overrideVersion = overrideProperties.get(artifactName);
                if (overrideVersion != null && !artifact.getVersion().equals(overrideVersion)) {
                    artifact.setVersion(overrideVersion);
                    overriddenArtifacts.put(artifactName, artifact);
                }
            }
        }
        return overriddenArtifacts;
    }

    @Override
    public Artifact getArtifact(String artifactName) {
        return artifactResolver.getArtifact(artifactName);
    }

    @Override
    public Set<String> getArtifactNames() {
        return artifactResolver.getArtifactNames();
    }

}
