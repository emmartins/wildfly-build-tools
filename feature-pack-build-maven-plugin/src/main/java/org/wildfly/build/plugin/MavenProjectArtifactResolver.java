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
import org.wildfly.build.MapArtifactResolver;

/**
 * An artifact resolver for a maven project's artifacts.
 * @author Eduardo Martins
 */
public class MavenProjectArtifactResolver extends MapArtifactResolver {

    public MavenProjectArtifactResolver(MavenProject mavenProject) {
        for (org.apache.maven.artifact.Artifact mavenProjectArtifact : mavenProject.getArtifacts()) {
            final String groupId = mavenProjectArtifact.getGroupId();
            final String artifactId = mavenProjectArtifact.getArtifactId();
            final String version = mavenProjectArtifact.getVersion();
            final String classifier = mavenProjectArtifact.getClassifier();
            final String extension = mavenProjectArtifact.getType();
            final Artifact artifact = new Artifact(groupId, artifactId, extension, classifier, version);
            StringBuilder sb = new StringBuilder();
            sb.append(groupId);
            sb.append(':');
            sb.append(artifactId);
            // the default artifact name's is groupId:artifactId
            final String defaultArtifactName = sb.toString();
            boolean onlyPutDefaultArtifactNameIfAbsent = false;
            if (extension != null && !extension.isEmpty()) {
                sb.append(":").append(extension);
                // put groupId:artifactId:extension
                artifactMap.put(sb.toString(), artifact);
                onlyPutDefaultArtifactNameIfAbsent = true;
            }
            if (classifier != null && !classifier.isEmpty()) {
                if (extension != null && !extension.isEmpty()) {
                    sb.append(':');
                } else {
                    sb.append("::");
                }
                sb.append(classifier);
                // put groupId:artifactId:extension:classifier or groupId:artifactId::classifier
                artifactMap.put(sb.toString(), artifact);
                onlyPutDefaultArtifactNameIfAbsent = true;
            }
            // put default artifact name if there is no extension and classifier, or such name is not yet in refs (avoids replacing the default without classifier)
            if (!onlyPutDefaultArtifactNameIfAbsent || !artifactMap.containsKey(defaultArtifactName)) {
                artifactMap.put(defaultArtifactName, artifact);
            }
        }
    }

}
