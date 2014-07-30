package org.wildfly.build.pack.model;

import org.wildfly.build.ArtifactResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eduardo Martins
 */
public class FeaturePackArtifactResolver implements ArtifactResolver {

    private final ArtifactResolver parent;
    private final Map<String, Artifact> artifactMap;

    public FeaturePackArtifactResolver(FeaturePackDescription featurePackDescription, ArtifactResolver parent) {
        this.parent = parent;
        this.artifactMap = new HashMap<>();
        for (Artifact artifact : featurePackDescription.getVersions().getArtifacts()) {
            StringBuilder sb = new StringBuilder();
            sb.append(artifact.getGACE().getGroupId());
            sb.append(':');
            sb.append(artifact.getGACE().getArtifactId());
            artifactMap.put(sb.toString(), artifact);
            if (artifact.getGACE().getClassifier() != null) {
                artifactMap.put(sb.append("::").append(artifact.getGACE().getClassifier()).toString(), artifact);
            }
        }
    }

    @Override
    public Artifact getArtifact(String artifactCoords) {
        Artifact artifact = parent.getArtifact(artifactCoords);
        if (artifact == null) {
            artifact = artifactMap.get(artifactCoords);
        }
        return artifact;
    }

    @Override
    public Artifact getArtifact(Artifact.GACE GACE) {
        StringBuilder sb = new StringBuilder();
        sb.append(GACE.getGroupId());
        sb.append(':');
        sb.append(GACE.getArtifactId());
        if (GACE.getClassifier() != null) {
            sb.append("::").append(GACE.getClassifier());
        }
        return getArtifact(sb.toString());
    }
}
