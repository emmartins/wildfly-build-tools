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

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.build.common.model.ArtifactRefsModelParser10;
import org.wildfly.build.common.model.ConfigFileOverride;
import org.wildfly.build.common.model.ConfigOverride;
import org.wildfly.build.common.model.CopyArtifactsModelParser10;
import org.wildfly.build.common.model.FileFilterModelParser10;
import org.wildfly.build.configassembly.SubsystemConfig;
import org.wildfly.build.configassembly.SubsystemsParser;
import org.wildfly.build.util.BuildPropertyReplacer;
import org.wildfly.build.util.MapPropertyResolver;
import org.wildfly.build.util.PropertyResolver;
import org.wildfly.build.util.xml.ParsingUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
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

    public static final String NAMESPACE_1_0 = "urn:wildfly:server-provisioning:1.0";

    enum Element {

        // default unknown element
        UNKNOWN(null),

        SERVER_PROVISIONING("server-provisioning"),
        FEATURE_PACKS("feature-packs"),
        FEATURE_PACK("feature-pack"),
        MODULES("modules"),
        CONFIG("config"),
        STANDALONE("standalone"),
        DOMAIN("domain"),
        PROPERTY("property"),
        SUBSYSTEMS("subsystems"),
        SUBSYSTEM("subsystem"),
        CONTENTS("contents"),
        ARTIFACT_REFS(ArtifactRefsModelParser10.ELEMENT_LOCAL_NAME),
        COPY_ARTIFACTS(CopyArtifactsModelParser10.ELEMENT_LOCAL_NAME),
        FILTER(FileFilterModelParser10.ELEMENT_LOCAL_NAME),
        ;

        private static final Map<QName, Element> elements;

        static {
            Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            elementsMap.put(new QName(NAMESPACE_1_0, SERVER_PROVISIONING.getLocalName()), SERVER_PROVISIONING);
            elementsMap.put(new QName(NAMESPACE_1_0, FEATURE_PACKS.getLocalName()), FEATURE_PACKS);
            elementsMap.put(new QName(NAMESPACE_1_0, FEATURE_PACK.getLocalName()), FEATURE_PACK);
            elementsMap.put(new QName(NAMESPACE_1_0, MODULES.getLocalName()), MODULES);
            elementsMap.put(new QName(NAMESPACE_1_0, FILTER.getLocalName()), FILTER);
            elementsMap.put(new QName(NAMESPACE_1_0, CONFIG.getLocalName()), CONFIG);
            elementsMap.put(new QName(NAMESPACE_1_0, STANDALONE.getLocalName()), STANDALONE);
            elementsMap.put(new QName(NAMESPACE_1_0, DOMAIN.getLocalName()), DOMAIN);
            elementsMap.put(new QName(NAMESPACE_1_0, PROPERTY.getLocalName()), PROPERTY);
            elementsMap.put(new QName(NAMESPACE_1_0, SUBSYSTEMS.getLocalName()), SUBSYSTEMS);
            elementsMap.put(new QName(NAMESPACE_1_0, SUBSYSTEM.getLocalName()), SUBSYSTEM);
            elementsMap.put(new QName(NAMESPACE_1_0, CONTENTS.getLocalName()), CONTENTS);
            elementsMap.put(new QName(NAMESPACE_1_0, ARTIFACT_REFS.getLocalName()), ARTIFACT_REFS);
            elementsMap.put(new QName(NAMESPACE_1_0, COPY_ARTIFACTS.getLocalName()), COPY_ARTIFACTS);
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
        PATTERN("pattern"),
        INCLUDE("include"),
        TRANSITIVE("transitive"),
        OUTPUT_FILE("output-file"),
        USE_TEMPLATE("use-template"),
        NAME("name"),
        VALUE("value"),
        ARTIFACT("artifact"),
        ;

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            attributesMap.put(new QName(PATTERN.getLocalName()), PATTERN);
            attributesMap.put(new QName(INCLUDE.getLocalName()), INCLUDE);
            attributesMap.put(new QName(TRANSITIVE.getLocalName()), TRANSITIVE);
            attributesMap.put(new QName(OUTPUT_FILE.getLocalName()), OUTPUT_FILE);
            attributesMap.put(new QName(USE_TEMPLATE.getLocalName()), USE_TEMPLATE);
            attributesMap.put(new QName(NAME.getLocalName()), NAME);
            attributesMap.put(new QName(VALUE.getLocalName()), VALUE);
            attributesMap.put(new QName(COPY_MODULE_ARTIFACTS.getLocalName()), COPY_MODULE_ARTIFACTS);
            attributesMap.put(new QName(EXTRACT_SCHEMAS.getLocalName()), EXTRACT_SCHEMAS);
            attributesMap.put(new QName(ARTIFACT.getLocalName()), ARTIFACT);


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

    private final BuildPropertyReplacer propertyReplacer;
    private final CopyArtifactsModelParser10 copyArtifactsModelParser;
    private final FileFilterModelParser10 fileFilterModelParser;
    private final ArtifactRefsModelParser10 artifactRefsModelParser;

    ServerProvisioningDescriptionModelParser10(PropertyResolver resolver) {
        this.propertyReplacer = new BuildPropertyReplacer(resolver);
        this.fileFilterModelParser = new FileFilterModelParser10(propertyReplacer);
        this.artifactRefsModelParser = new ArtifactRefsModelParser10(this.propertyReplacer);
        this.copyArtifactsModelParser = new CopyArtifactsModelParser10(this.propertyReplacer, this.fileFilterModelParser, artifactRefsModelParser);
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
                        case ARTIFACT_REFS:
                            artifactRefsModelParser.parseArtifactRefs(reader, result.getArtifactRefs());
                            break;
                        case COPY_ARTIFACTS:
                            copyArtifactsModelParser.parseCopyArtifacts(reader, result.getCopyArtifacts(), result.getArtifactRefs());
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
                        case FEATURE_PACK:
                            parseFeaturePack(reader, result);
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

    private void parseFeaturePack(final XMLStreamReader reader, final ServerProvisioningDescription result) throws XMLStreamException {
        String artifact = null;
        final Set<Attribute> required = EnumSet.of(Attribute.ARTIFACT);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case ARTIFACT:
                    artifact = artifactRefsModelParser.parseArtifactName(propertyReplacer.replaceProperties(reader.getAttributeValue(i)), "zip", result.getArtifactRefs());
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        ServerProvisioningDescription.FeaturePack.ModuleFilters moduleFilters = null;
        ConfigOverride config = null;
        ServerProvisioningDescription.FeaturePack.ContentFilters contentFilters = null;
        List<ServerProvisioningDescription.FeaturePack.Subsystem> subsystems = null;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    result.getFeaturePacks().add(new ServerProvisioningDescription.FeaturePack(artifact, moduleFilters, config, contentFilters, subsystems));
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case MODULES:
                            moduleFilters = parseModules(reader);
                            break;
                        case CONFIG:
                            if (subsystems != null) {
                                throw new XMLStreamException("server provisioning xml specifies a feature pack filtered by config and subsystems");
                            }
                            config = new ConfigOverride();
                            parseConfig(reader, config);
                            break;
                        case CONTENTS:
                            contentFilters = parseContents(reader);
                            break;
                        case SUBSYSTEMS:
                            if (config != null) {
                                throw new XMLStreamException("server provisioning xml specifies a feature pack filtered by config and subsystems");
                            }
                            subsystems = new ArrayList<>();
                            parseSubsystems(reader, subsystems);
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

    private ServerProvisioningDescription.FeaturePack.ContentFilters parseContents(XMLStreamReader reader) throws XMLStreamException {
        boolean include = true;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case INCLUDE:
                    include = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        ServerProvisioningDescription.FeaturePack.ContentFilters result = new ServerProvisioningDescription.FeaturePack.ContentFilters(include);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return result;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FILTER:
                            fileFilterModelParser.parseFilter(reader, result.getFilters());
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

    private ServerProvisioningDescription.FeaturePack.ModuleFilters parseModules(final XMLStreamReader reader) throws XMLStreamException {
        boolean include = true;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case INCLUDE:
                    include = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        ServerProvisioningDescription.FeaturePack.ModuleFilters result = new ServerProvisioningDescription.FeaturePack.ModuleFilters(include);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return result;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FILTER:
                            parseModuleFilter(reader, result.getFilters());
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

    private void parseModuleFilter(final XMLStreamReader reader, List<ModuleFilter> result) throws XMLStreamException {
        String pattern = null;
        boolean include = false;
        boolean transitive = true;
        final Set<Attribute> required = EnumSet.of(Attribute.PATTERN, Attribute.INCLUDE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case PATTERN:
                    pattern = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case INCLUDE:
                    include = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                case TRANSITIVE:
                    transitive = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }

        ParsingUtils.parseNoContent(reader);

        result.add(new ModuleFilter(pattern, include, transitive));
    }

    public void parseConfig(final XMLStreamReader reader, ConfigOverride result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case STANDALONE:
                            parseConfigFile(reader, result.getStandaloneConfigFiles());
                            break;
                        case DOMAIN:
                            parseConfigFile(reader, result.getDomainConfigFiles());
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

    private void parseConfigFile(XMLStreamReader reader, Map<String, ConfigFileOverride> result) throws XMLStreamException {
        boolean useTemplate = false;
        String outputFile = null;
        final Set<Attribute> required = EnumSet.of(Attribute.OUTPUT_FILE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case USE_TEMPLATE:
                    useTemplate = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                case OUTPUT_FILE:
                    outputFile = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        final Map<String, String> properties = new HashMap<>();
        final BuildPropertyReplacer subystemsParserPropertyReplacer = new BuildPropertyReplacer(new MapPropertyResolver(properties));
        Map<String, Map<String, SubsystemConfig>> subsystems = null;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    result.put(outputFile, new ConfigFileOverride(properties, useTemplate, subsystems, outputFile));
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PROPERTY:
                            parseProperty(reader, properties);
                            break;
                        case SUBSYSTEMS:
                            if (subsystems == null) {
                                subsystems = new HashMap<>();
                            }
                            SubsystemsParser.parseSubsystems(reader, subystemsParserPropertyReplacer, subsystems);
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

    private void parseProperty(XMLStreamReader reader, Map<String, String> result) throws XMLStreamException {
        String name = null;
        String value = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case VALUE:
                    value = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        ParsingUtils.parseNoContent(reader);
        result.put(name, value);
    }

    private void parseSubsystems(final XMLStreamReader reader, List<ServerProvisioningDescription.FeaturePack.Subsystem> result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case SUBSYSTEM:
                            result.add(parseSubsystem(reader));
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

    private ServerProvisioningDescription.FeaturePack.Subsystem parseSubsystem(final XMLStreamReader reader) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        boolean transitive = false;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case TRANSITIVE:
                    transitive = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        ParsingUtils.parseNoContent(reader);

        return new ServerProvisioningDescription.FeaturePack.Subsystem(name, transitive);
    }

}
