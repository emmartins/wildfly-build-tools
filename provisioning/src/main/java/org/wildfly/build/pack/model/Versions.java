package org.wildfly.build.pack.model;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author Eduardo Martins
 */
public class Versions {

    private final Set<Artifact> artifacts = new TreeSet<>();

    public Set<Artifact> getArtifacts() {
        return artifacts;
    }
}
