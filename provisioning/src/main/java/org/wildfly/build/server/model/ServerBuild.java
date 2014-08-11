package org.wildfly.build.server.model;

import org.wildfly.build.util.ZipFileSubsystemInputStreamSources;
import org.wildfly.build.pack.model.FeaturePack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Eduardo Martins
 */
public class ServerBuild {

    private final ServerBuildDescription description;

    /**
     * the server's feature packs
     */
    private final List<FeaturePack> featurePacks = new ArrayList<>();

    /**
     * each feature pack's subsystem inputstream source's resolver
     */
    private final Map<FeaturePack, ZipFileSubsystemInputStreamSources> inputStreamSourceResolverMap = new HashMap<>();

    /**
     *
     * @param description
     */
    public ServerBuild(ServerBuildDescription description) {
        this.description = description;
    }

    /**
     *
     * @return
     */
    public ServerBuildDescription getDescription() {
        return description;
    }

    /**
     *
     * @return
     */
    public List<FeaturePack> getFeaturePacks() {
        return featurePacks;
    }

    /**
     *
     * @return
     */
    public Map<FeaturePack, ZipFileSubsystemInputStreamSources> getSubsystemInputStreamSourcesMap() {
        return inputStreamSourceResolverMap;
    }
}
