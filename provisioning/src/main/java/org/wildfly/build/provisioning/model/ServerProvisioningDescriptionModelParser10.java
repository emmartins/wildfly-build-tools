/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.build.provisioning.model;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.build.common.model.CopyArtifactsModelParser10;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.util.BuildPropertyReplacer;
import org.wildfly.build.util.PropertyResolver;
import org.wildfly.build.util.xml.ParsingUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses the distribution build config file, i.e. the config file that is
 * used to create a wildfly distribution.
 *
 * @author Eduardo Martins
 */
class ServerProvisioningDescriptionModelParser10 implements XMLElementReader<ServerProvisioningDescription> {


    private final BuildPropertyReplacer propertyReplacer;
    private final CopyArtifactsModelParser10 copyArtifactsModelParser;

    public static final String NAMESPACE_1_0 = "urn:wildfly:server-provisioning:1.0";

    enum Element {

        // default unknown element
        UNKNOWN(null),

        SERVER_PROVISIONING("server-provisioning"),
        FEATURE_PACKS("feature-packs"),
        ARTIFACT("artifact"),
        VERSION_OVERRIDES("version-overrides"),
        VERSION_OVERRIDE("version-override"),
        COPY_ARTIFACTS(CopyArtifactsModelParser10.ELEMENT_LOCAL_NAME),
        ;


        private static final Map<QName, Element> elements;

        static {
            Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            elementsMap.put(new QName(NAMESPACE_1_0, Element.SERVER_PROVISIONING.getLocalName()), Element.SERVER_PROVISIONING);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.FEATURE_PACKS.getLocalName()), Element.FEATURE_PACKS);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.ARTIFACT.getLocalName()), Element.ARTIFACT);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.VERSION_OVERRIDES.getLocalName()), Element.VERSION_OVERRIDES);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.VERSION_OVERRIDE.getLocalName()), Element.VERSION_OVERRIDE);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.COPY_ARTIFACTS.getLocalName()), Element.COPY_ARTIFACTS);
            elements = elementsMap;
        }

        static Element of(QName qName) {
            QName name;
            if (qName.getNamespaceURI().equals("")) {
                name = new QName(NAMESPACE_1_0, qName.getLocalPart());
            } else {
                name = qName;
            }
            final Element element = elements.get(name);
            return element == null ? UNKNOWN : element;
        }

        private final String name;

        Element(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }
    }

    enum Attribute {

        // default unknown attribute
        UNKNOWN(null),

        COPY_MODULE_ARTIFACTS("copy-module-artifacts"),
        EXTRACT_SCHEMAS("extract-schemas"),
        NAME("name"),
        GROUP_ID("groupId"),
        ARTIFACT_ID("artifactId"),
        CLASSIFIER("classifier"),
        EXTENSION("extension"),
        VERSION("version")
        ;

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            attributesMap.put(new QName(NAME.getLocalName()), NAME);
            attributesMap.put(new QName(COPY_MODULE_ARTIFACTS.getLocalName()), COPY_MODULE_ARTIFACTS);
            attributesMap.put(new QName(EXTRACT_SCHEMAS.getLocalName()), EXTRACT_SCHEMAS);
            attributesMap.put(new QName(GROUP_ID.getLocalName()), GROUP_ID);
            attributesMap.put(new QName(ARTIFACT_ID.getLocalName()), ARTIFACT_ID);
            attributesMap.put(new QName(CLASSIFIER.getLocalName()), CLASSIFIER);
            attributesMap.put(new QName(EXTENSION.getLocalName()), EXTENSION);
            attributesMap.put(new QName(VERSION.getLocalName()), VERSION);

            attributes = attributesMap;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }

        private final String name;

        Attribute(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }
    }

    ServerProvisioningDescriptionModelParser10(PropertyResolver resolver) {
        this.propertyReplacer = new BuildPropertyReplacer(resolver);
        this.copyArtifactsModelParser = new CopyArtifactsModelParser10(this.propertyReplacer);
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final ServerProvisioningDescription result) throws XMLStreamException {
        final Set<Attribute> required = EnumSet.noneOf(Attribute.class);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case COPY_MODULE_ARTIFACTS:
                    result.setCopyModuleArtifacts(Boolean.parseBoolean(reader.getAttributeValue(i)));
                    break;
                case EXTRACT_SCHEMAS:
                    result.setExtractSchemas(Boolean.parseBoolean(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());

                    switch (element) {
                        case FEATURE_PACKS:
                            parseFeaturePacks(reader, result);
                            break;
                        case VERSION_OVERRIDES:
                            parseVersionOverrides(reader, result);
                            break;
                        case COPY_ARTIFACTS:
                            copyArtifactsModelParser.parseCopyArtifacts(reader, result.getCopyArtifacts());
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }

    private void parseFeaturePacks(final XMLStreamReader reader, final ServerProvisioningDescription result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case ARTIFACT:
                            parseArtifact(reader, result.getFeaturePacks(), "zip"); //feature packs default to zip
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }


    private void parseVersionOverrides(final XMLStreamReader reader, final ServerProvisioningDescription result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case VERSION_OVERRIDE:
                            parseArtifact(reader, result.getVersionOverrides(), null);
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }


    private void parseArtifact(final XMLStreamReader reader, List<Artifact> overrides, String defaultExtension) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String artifact = null;
        String version = null;
        String groupId = null;
        String classifier = null;
        String extension = defaultExtension;
        final Set<Attribute> required = EnumSet.of(Attribute.ARTIFACT_ID, Attribute.VERSION, Attribute.GROUP_ID);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case GROUP_ID:
                    groupId = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case ARTIFACT_ID:
                    artifact = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case VERSION:
                    version = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case CLASSIFIER:
                    classifier = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case EXTENSION:
                    extension = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        ParsingUtils.parseNoContent(reader);

        overrides.add(new Artifact(groupId, artifact, classifier, extension, version));
    }

}
