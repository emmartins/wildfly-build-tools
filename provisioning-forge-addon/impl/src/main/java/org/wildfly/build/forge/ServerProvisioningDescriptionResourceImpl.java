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

import org.jboss.forge.addon.resource.AbstractFileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceException;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;
import org.wildfly.build.provisioning.model.ServerProvisioningDescriptionModelParser;
import org.wildfly.build.provisioning.model.ServerProvisioningDescriptionModelXMLWriter10;
import org.wildfly.build.util.MapPropertyResolver;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The impl of {@link org.wildfly.build.forge.ServerProvisioningDescriptionResource}.
 * @author Eduardo Martins
 */
public class ServerProvisioningDescriptionResourceImpl extends AbstractFileResource<ServerProvisioningDescriptionResource> implements ServerProvisioningDescriptionResource {

    /**
     * the description within the resource
     */
    private volatile ServerProvisioningDescription description;

    /**
     *
     * @param factory
     * @param file
     */
    public ServerProvisioningDescriptionResourceImpl(final ResourceFactory factory, final File file) {
        super(factory, file);
    }

    @Override
    public Resource<File> createFrom(File file) {
        return new ServerProvisioningDescriptionResourceImpl(getResourceFactory(), file);
    }

    @Override
    public Resource<?> getChild(String name) {
        for (Resource<?> child : listResources()) {
            if (child.getName().trim().equals(name)) {
                return child;
            }
        }
        return null;
    }

    @Override
    protected List<Resource<?>> doListResources() {
        final List<Resource<?>> children = new ArrayList<>();
        listFeaturePacks(children);
        // TODO implement other inner resources
        return children;
    }

    private void listFeaturePacks(List<Resource<?>> children) {
        List<ServerProvisioningDescription.FeaturePack> featurePacks = getServerProvisioningDescription().getFeaturePacks();
        if (featurePacks != null) {
            for (ServerProvisioningDescription.FeaturePack featurePack : featurePacks) {
                children.add(new ServerProvisioningDescriptionFeaturePackResourceImpl(getResourceFactory(), this, featurePack));
            }
        }
    }

    @Override
    public ServerProvisioningDescriptionResource setContents(final ServerProvisioningDescription source) {
        try {
            ServerProvisioningDescriptionModelXMLWriter10.INSTANCE.write(source, getUnderlyingResourceObject());
            return this;
        } catch (Throwable e) {
            throw new ResourceException("Failed to write the server provisioning description", e);
        }
    }

    @Override
    public ServerProvisioningDescriptionResource setContents() {
        return setContents(description);
    }

    @Override
    public synchronized ServerProvisioningDescription getServerProvisioningDescription() {
        if (description == null) {
            description = parseServerProvisioningDescription();
        }
        return description;
    }

    private ServerProvisioningDescription parseServerProvisioningDescription() {
        try (InputStream in = getResourceInputStream()) {
            return new ServerProvisioningDescriptionModelParser(new MapPropertyResolver(System.getProperties())).parse(in);
        } catch (Throwable e) {
            throw new ResourceException("Failed to read the server provisioning description", e);
        }
    }
}
