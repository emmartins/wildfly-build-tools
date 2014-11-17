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
package org.wildfly.build.forge;

import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.VirtualResource;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The impl of {@link org.wildfly.build.forge.ServerProvisioningDescriptionFeaturePackResource}.
 * @author Eduardo Martins
 */
public class ServerProvisioningDescriptionFeaturePackResourceImpl extends VirtualResource<ServerProvisioningDescription.FeaturePack> implements ServerProvisioningDescriptionFeaturePackResource {

    /**
     * the featurePack within the resource
     */
    private final ServerProvisioningDescription.FeaturePack featurePack;
    private final ServerProvisioningDescriptionResource descriptionResource;

    /**
     *
     * @param factory
     * @param parent
     * @param featurePack
     */
    public ServerProvisioningDescriptionFeaturePackResourceImpl(final ResourceFactory factory, final ServerProvisioningDescriptionResource parent, ServerProvisioningDescription.FeaturePack featurePack) {
        super(factory, parent);
        this.featurePack = featurePack;
        this.descriptionResource = parent;
    }

    @Override
    public String getName() {
        return "feature-packs/"+featurePack.getArtifact();
    }

    @Override
    public Resource<?> getChild(String name) {
        return null;
    }

    @Override
    protected List<Resource<?>> doListResources() {
        return Collections.emptyList();
    }

    @Override
    public ServerProvisioningDescription.FeaturePack getUnderlyingResourceObject() {
        return featurePack;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean delete() throws UnsupportedOperationException {
        final Iterator<ServerProvisioningDescription.FeaturePack> iterator = descriptionResource.getServerProvisioningDescription().getFeaturePacks().iterator();
        while(iterator.hasNext()) {
            final ServerProvisioningDescription.FeaturePack featurePack = iterator.next();
            if (featurePack.getArtifact().equals(this.featurePack.getArtifact())) {
                iterator.remove();
                descriptionResource.setContents();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean delete(boolean recursive) throws UnsupportedOperationException {
        return delete();
    }
}