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
package org.wildfly.build.test;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.build.Artifact;
import org.wildfly.build.BuildArtifactResolver;
import org.wildfly.build.MapArtifactResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * {@link org.wildfly.build.BuildArtifactResolver} tests.
 * @author Eduardo Martins
 */
public class BuildArtifactResolverTestCase {

    @Test
    public void testPropertyVersionOverride() {
        final Artifact a1 = new Artifact("g", "a1", "1.0");
        final String a1Name = a1.getGroupId()+':'+a1.getArtifactId();
        final Map<String, Artifact> artifactMap1 = new HashMap<>();
        artifactMap1.put(a1Name, a1);
        final MapArtifactResolver artifactResolver1 = new MapArtifactResolver(artifactMap1);

        final Artifact a2 = new Artifact("g", "a2", "1.0");
        final String a2Name = a2.getGroupId()+':'+a2.getArtifactId();
        final Map<String, Artifact> artifactMap2 = new HashMap<>();
        artifactMap2.put(a2Name, a2);
        final MapArtifactResolver artifactResolver2 = new MapArtifactResolver(artifactMap2);

        final Properties properties = new Properties();
        final String overrideVersion = "2.0";
        properties.setProperty(BuildArtifactResolver.OVERRIDE_PROPERTY_NAME_PREFIX+a1Name, overrideVersion);
        properties.setProperty(BuildArtifactResolver.OVERRIDE_PROPERTY_NAME_PREFIX+a2Name, overrideVersion);

        final BuildArtifactResolver buildArtifactResolver = new BuildArtifactResolver(properties, artifactResolver1);
        Assert.assertEquals(buildArtifactResolver.getArtifact(a1Name).getVersion(), overrideVersion);
        buildArtifactResolver.override(artifactResolver2);
        Assert.assertEquals(artifactResolver2.getArtifact(a2Name).getVersion(), overrideVersion);
    }

    @Test
    public void testArtifactOverride() {
        final Artifact artifact = new Artifact("groupId", "artifactId", "1.0");
        final String artifactName = artifact.getGroupId()+':'+artifact.getArtifactId();
        final Map<String, Artifact> artifactMap = new HashMap<>();
        artifactMap.put(artifactName, artifact);
        final MapArtifactResolver artifactResolver = new MapArtifactResolver(artifactMap);

        final Artifact overrideArtifact = new Artifact(artifact.getGroupId(), artifact.getArtifactId(), "extension", "classifier", "2.0");
        final Map<String, Artifact> overrideArtifactMap = new HashMap<>();
        overrideArtifactMap.put(artifactName, overrideArtifact);
        final MapArtifactResolver overrideArtifactResolver = new MapArtifactResolver(overrideArtifactMap);

        final BuildArtifactResolver buildArtifactResolver = new BuildArtifactResolver(null, overrideArtifactResolver);
        Assert.assertEquals(buildArtifactResolver.getArtifact(artifactName).getVersion(), overrideArtifact.getVersion());
        buildArtifactResolver.override(artifactResolver);
        final Artifact resolvedArtifact = artifactResolver.getArtifact(artifactName);
        Assert.assertNotNull(resolvedArtifact);
        Assert.assertEquals(resolvedArtifact.getGroupId(), overrideArtifact.getGroupId());
        Assert.assertEquals(resolvedArtifact.getArtifactId(), overrideArtifact.getArtifactId());
        Assert.assertEquals(resolvedArtifact.getExtension(), overrideArtifact.getExtension());
        Assert.assertEquals(resolvedArtifact.getClassifier(), overrideArtifact.getClassifier());
        Assert.assertEquals(resolvedArtifact.getVersion(), overrideArtifact.getVersion());
    }
}
