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
package org.wildfly.build.provisioning.model;

import org.wildfly.build.common.model.ArtifactRefsXMLWriter10;
import org.wildfly.build.common.model.CopyArtifactsXMLWriter10;
import org.wildfly.build.util.xml.AttributeValue;
import org.wildfly.build.util.xml.ElementNode;
import org.wildfly.build.util.xml.FormattingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.wildfly.build.provisioning.model.ServerProvisioningDescriptionModelParser10.Attribute;
import static org.wildfly.build.provisioning.model.ServerProvisioningDescriptionModelParser10.Element;

/**
 * Writes a {@link org.wildfly.build.provisioning.model.ServerProvisioningDescription} as XML.
 *
 * @author Eduardo Martins
 */
public class ServerProvisioningDescriptionModelXMLWriter10 {

    public static final ServerProvisioningDescriptionModelXMLWriter10 INSTANCE = new ServerProvisioningDescriptionModelXMLWriter10();

    private ServerProvisioningDescriptionModelXMLWriter10() {
    }

    public void write(ServerProvisioningDescription description, File outputFile) throws XMLStreamException, IOException {
        final ElementNode rootElementNode = new ElementNode(null, Element.SERVER_PROVISIONING.getLocalName(), ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0);
        processFeaturePacks(description.getFeaturePacks(), rootElementNode);
        ArtifactRefsXMLWriter10.INSTANCE.write(description.getArtifactRefs(), rootElementNode);
        CopyArtifactsXMLWriter10.INSTANCE.write(description.getCopyArtifacts(), rootElementNode);
        FormattingXMLStreamWriter writer = new FormattingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(new BufferedWriter(new FileWriter(outputFile))));
        try {
            writer.writeStartDocument();
            rootElementNode.marshall(writer);
            writer.writeEndDocument();
        } finally {
            try {
                writer.close();
            } catch (Exception ignore) {
            }
        }
    }

    protected void processFeaturePacks(List<ServerProvisioningDescription.FeaturePack> featurePacks, ElementNode parentElementNode) {
        if (!featurePacks.isEmpty()) {
            final ElementNode featurePacksElementNode = new ElementNode(parentElementNode, Element.FEATURE_PACKS.getLocalName());
            for (ServerProvisioningDescription.FeaturePack featurePack : featurePacks) {
                final ElementNode featurePackElementNode = new ElementNode(featurePacksElementNode, Element.FEATURE_PACK.getLocalName());
                featurePackElementNode.addAttribute(Attribute.ARTIFACT.getLocalName(), new AttributeValue(featurePack.getArtifact()));
                // TODO implement writing of inner elements
                featurePacksElementNode.addChild(featurePackElementNode);
            }
            parentElementNode.addChild(featurePacksElementNode);
        }
    }

}
