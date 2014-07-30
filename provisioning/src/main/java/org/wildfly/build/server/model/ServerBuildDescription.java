package org.wildfly.build.server.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of the server build config
 *
 * @author Eduardo Martins
 */
public class ServerBuildDescription {

    private final List<String> featurePacks = new ArrayList<>();

    public List<String> getFeaturePacks() {
        return featurePacks;
    }
}
