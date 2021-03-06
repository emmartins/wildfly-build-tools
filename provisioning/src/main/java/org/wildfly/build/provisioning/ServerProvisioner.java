/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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

package org.wildfly.build.provisioning;

import org.jboss.logging.Logger;
import org.wildfly.build.ArtifactFileResolver;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.Locations;
import org.wildfly.build.configassembly.ConfigurationAssembler;
import org.wildfly.build.configassembly.SubsystemInputStreamSources;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.pack.model.Config;
import org.wildfly.build.pack.model.ConfigFile;
import org.wildfly.build.pack.model.CopyArtifact;
import org.wildfly.build.pack.model.FeaturePack;
import org.wildfly.build.pack.model.FeaturePackFactory;
import org.wildfly.build.pack.model.FilePermission;
import org.wildfly.build.provisioning.model.ServerProvisioning;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;
import org.wildfly.build.util.BuildPropertyReplacer;
import org.wildfly.build.util.FileUtils;
import org.wildfly.build.util.MapPropertyResolver;
import org.wildfly.build.util.ModuleArtifactPropertyResolver;
import org.wildfly.build.util.ModuleParseResult;
import org.wildfly.build.util.ModuleParser;
import org.wildfly.build.util.ZipEntryInputStreamSource;
import org.wildfly.build.util.ZipFileSubsystemInputStreamSources;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Task that builds a server from a set of features packs declared in the pack.
 *
 * @author Eduardo Martins
 */
public class ServerProvisioner {

    private static final Logger logger = Logger.getLogger(ServerProvisioner.class);

    private static final String SUBSYSTEM_TEMPLATES_ENTRY_PREFIX = "subsystem-templates" + File.separator;
    private static final String SUBSYSTEM_SCHEMA_ENTRY_PREFIX = "schema" + File.separator;
    private static final String SUBSYSTEM_SCHEMA_TARGET_DIRECTORY = "docs" + File.separator + "schema";

    private static final boolean OS_WINDOWS = System.getProperty("os.name").contains("indows");

    public static void build(ServerProvisioningDescription description, File outputDirectory, ArtifactResolver buildArtifactResolver, ArtifactFileResolver artifactFileResolver) {
        final ServerProvisioning serverProvisioning = new ServerProvisioning(description);
        final List<String> errors = new ArrayList<>();
        try {
            // create the feature packs
            for (String featurePackArtifactCoords : description.getFeaturePacks()) {
                final FeaturePack featurePack = FeaturePackFactory.createPack(featurePackArtifactCoords, buildArtifactResolver, artifactFileResolver);
                serverProvisioning.getFeaturePacks().add(featurePack);
            }
            // create output dir
            FileUtils.deleteRecursive(outputDirectory);
            outputDirectory.mkdirs();
            final Set<String> filesProcessed = new HashSet<>();
            // process modules (needs to be done for all feature packs before any config is processed)
            for (FeaturePack featurePack : serverProvisioning.getFeaturePacks()) {
                processFeaturePackModules(featurePack, serverProvisioning, outputDirectory, filesProcessed, artifactFileResolver);
            }
            // process everything else from feature pack
            for (FeaturePack featurePack : serverProvisioning.getFeaturePacks()) {
                processConfig(featurePack, serverProvisioning, outputDirectory, filesProcessed);
                processCopyArtifacts(featurePack, outputDirectory, filesProcessed, artifactFileResolver);
                extractFeaturePackContents(featurePack, outputDirectory, filesProcessed);
                processFilePermissions(featurePack, outputDirectory);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            if(!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Some errors were encountered creating the feature pack\n");
                for(String error : errors) {
                    sb.append(error);
                    sb.append("\n");
                }
                throw new RuntimeException(sb.toString());
            }
        }
    }

    private static void processFeaturePackModules(FeaturePack featurePack, ServerProvisioning serverProvisioning, File outputDirectory, Set<String> filesProcessed, ArtifactFileResolver artifactFileResolver) throws IOException {
        // create the feature pack's subsystem parser factory and store it in the server builder
        ZipFileSubsystemInputStreamSources inputStreamSourceResolver = new ZipFileSubsystemInputStreamSources();
        serverProvisioning.getSubsystemInputStreamSourcesMap().put(featurePack, inputStreamSourceResolver);
        final boolean thinServer = !serverProvisioning.getDescription().isCopyModuleArtifacts();
        final boolean extractSchemas = serverProvisioning.getDescription().isExtractSchemas();
        // create the module's artifact property replacer
        final BuildPropertyReplacer buildPropertyReplacer = thinServer ? new BuildPropertyReplacer(new ModuleArtifactPropertyResolver(featurePack.getArtifactResolver())) : null;
        // create target dir for subsystem schemas if does not exists yet
        final File schemaOutputDirectory = new File(outputDirectory, SUBSYSTEM_SCHEMA_TARGET_DIRECTORY);
        if (!schemaOutputDirectory.exists()) {
            schemaOutputDirectory.mkdirs();
        }
        // process each module file
        try (JarFile jar = new JarFile(featurePack.getFeaturePackFile())) {
            for (String jarEntryName : featurePack.getModulesFiles()) {
                if (!filesProcessed.add(jarEntryName)) {
                    continue;
                }
                File targetFile = new File(outputDirectory, jarEntryName);
                // extract the file
                FileUtils.extractFile(jar, jarEntryName, targetFile);
                // if file is module xml process it
                if (jarEntryName.endsWith(File.separator+"module.xml")) {
                    try {
                        // read module xml to string for content update
                        String moduleXmlContents = FileUtils.readFile(targetFile);
                        // parse the module xml
                        ModuleParseResult result = ModuleParser.parse(targetFile.toPath());
                        // process module artifacts
                        for (String artifactName : result.getArtifacts()) {
                            if(artifactName.startsWith("${") && artifactName.endsWith("}")) {
                                String artifactCoords = artifactName.substring(2, artifactName.length() - 1);
                                Artifact artifact = featurePack.getArtifactResolver().getArtifact(artifactCoords);
                                if (artifact == null) {
                                    throw new RuntimeException("Could not resolve module resource artifact " + artifactName + " for feature pack "+ featurePack.getFeaturePackFile());
                                }
                                try {
                                    // process the module artifact
                                    File artifactFile = artifactFileResolver.getArtifactFile(artifact);
                                    try (ZipFile zip = new ZipFile(artifactFile)) {
                                        // extract subsystem template and schema, if present
                                        if (zip.getEntry("subsystem-templates") != null || zip.getEntry("schema") != null) {
                                            Enumeration<? extends ZipEntry> entries = zip.entries();
                                            while (entries.hasMoreElements()) {
                                                ZipEntry entry = entries.nextElement();
                                                if (!entry.isDirectory()) {
                                                    String entryName = entry.getName();
                                                    if (entryName.startsWith(SUBSYSTEM_TEMPLATES_ENTRY_PREFIX)) {
                                                        inputStreamSourceResolver.addSubsystemFileSource(entryName.substring(SUBSYSTEM_TEMPLATES_ENTRY_PREFIX.length()), artifactFile, entry);
                                                    } else if (extractSchemas && entryName.startsWith(SUBSYSTEM_SCHEMA_ENTRY_PREFIX)) {
                                                        try (InputStream in = zip.getInputStream(entry)) {
                                                            FileUtils.copyFile(in, new File(schemaOutputDirectory, entryName.substring(SUBSYSTEM_SCHEMA_ENTRY_PREFIX.length())));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (!thinServer) {
                                        // copy the artifact
                                        String artifactFileName = artifactFile.getName();
                                        FileUtils.copyFile(artifactFile, new File(targetFile.getParent(), artifactFileName));
                                        // update module xml content
                                        moduleXmlContents = moduleXmlContents.replaceAll("<artifact\\s+name=\"\\$\\{" + artifactCoords + "\\}\"\\s*/>", "<resource-root path=\"" + artifactFileName + "\"/>");
                                    }
                                } catch (Throwable t) {
                                    throw new RuntimeException("Could not extract resources from " + artifactName, t);
                                }
                            } else {
                                getLog().error("Hard coded artifact " + artifactName);
                            }
                        }
                        if (thinServer) {
                            // replace artifact coords properties with the ones expected by jboss-modules
                            moduleXmlContents = buildPropertyReplacer.replaceProperties(moduleXmlContents);
                        }
                        // write updated module xml content
                        FileUtils.copyFile(new ByteArrayInputStream(moduleXmlContents.getBytes("UTF-8")), targetFile);
                    } catch (XMLStreamException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to process feature pack " + featurePack.getFeaturePackFile() + " modules", e);
        }
        for (FeaturePack dependency : featurePack.getDependencies()) {
            // process modules of the dependency
            processFeaturePackModules(dependency, serverProvisioning, outputDirectory, filesProcessed, artifactFileResolver);
            // get the dependency subsystem parser factory and merge its entries
            ZipFileSubsystemInputStreamSources dependencySubsystemParserFactory = serverProvisioning.getSubsystemInputStreamSourcesMap().get(dependency);
            inputStreamSourceResolver.addAllSubsystemFileSources(dependencySubsystemParserFactory);
        }
    }

    private static void processConfig(FeaturePack featurePack, ServerProvisioning serverProvisioning, File outputDirectory, Set<String> filesProcessed) throws IOException, XMLStreamException {
        final SubsystemInputStreamSources subsystemInputStreamSources = serverProvisioning.getSubsystemInputStreamSourcesMap().get(featurePack);
        final Config config = featurePack.getDescription().getConfig();
        try (ZipFile zipFile = new ZipFile(featurePack.getFeaturePackFile())) {
            for (ConfigFile configFile : config.getDomainConfigFiles()) {
                processConfigFile(configFile, featurePack, zipFile, subsystemInputStreamSources, "domain", outputDirectory, filesProcessed);
            }
            for (ConfigFile configFile : config.getStandaloneConfigFiles()) {
                processConfigFile(configFile, featurePack, zipFile, subsystemInputStreamSources, "server", outputDirectory, filesProcessed);
            }
        }
        for (FeaturePack dependency : featurePack.getDependencies()) {
            processConfig(dependency, serverProvisioning, outputDirectory, filesProcessed);
        }
    }

    private static void processConfigFile(ConfigFile configFile, FeaturePack featurePack, ZipFile zipFile, SubsystemInputStreamSources subsystemInputStreamSources, String templateRootElementName, File outputDirectory, Set<String> filesProcessed) throws IOException, XMLStreamException {
        if (!filesProcessed.add(configFile.getOutputFile())) {
            return;
        }
        ZipEntry templateFileZipEntry = zipFile.getEntry(configFile.getTemplate());
        if (templateFileZipEntry == null) {
            throw new RuntimeException("Feature pack "+featurePack.getFeaturePackFile()+" template file "+configFile.getTemplate()+" not found");
        }
        ZipEntry subsystemsFileZipEntry = zipFile.getEntry(configFile.getSubsystems());
        if (subsystemsFileZipEntry == null) {
            throw new RuntimeException("Feature pack "+featurePack.getFeaturePackFile()+" subsystems file "+configFile.getSubsystems()+" not found");
        }
        new ConfigurationAssembler(subsystemInputStreamSources,
                new ZipEntryInputStreamSource(featurePack.getFeaturePackFile(), templateFileZipEntry),
                templateRootElementName,
                new ZipEntryInputStreamSource(featurePack.getFeaturePackFile(), subsystemsFileZipEntry),
                new File(outputDirectory, configFile.getOutputFile()),
                new MapPropertyResolver(configFile.getProperties()))
                .assemble();
    }

    private static void processCopyArtifacts(FeaturePack featurePack, File outputDirectory, Set<String> filesProcessed, ArtifactFileResolver artifactFileResolver) throws IOException {
        for (CopyArtifact copyArtifact : featurePack.getDescription().getCopyArtifacts()) {
            if (!filesProcessed.add(copyArtifact.getToLocation())) {
                continue;
            }
            File target = new File(outputDirectory, copyArtifact.getToLocation());
            if (!target.getParentFile().isDirectory()) {
                if (!target.getParentFile().mkdirs()) {
                    throw new IOException("Could not create directory " + target.getParentFile());
                }
            }
            Artifact artifact = featurePack.getArtifactResolver().getArtifact(copyArtifact.getArtifact());
            if (artifact == null) {
                throw new RuntimeException("Could not resolve artifact " + copyArtifact.getArtifact() + " to copy");
            }
            File artifactFile = artifactFileResolver.getArtifactFile(artifact);
            if (artifactFile == null) {
                throw new RuntimeException("Could not resolve file for artifact " + copyArtifact.getArtifact() + " to copy");
            }
            if (copyArtifact.isExtract()) {
                extractArtifact(artifactFile, target, copyArtifact);
            } else {
                FileUtils.copyFile(artifactFile, target);
            }
        }
        for (FeaturePack dependency : featurePack.getDependencies()) {
            processCopyArtifacts(dependency, outputDirectory, filesProcessed, artifactFileResolver);
        }
    }

    private static void extractArtifact(File file, File target, CopyArtifact copy) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (copy.includeFile(entry.getName())) {
                    if (entry.isDirectory()) {
                        new File(target, entry.getName()).mkdirs();
                    } else {
                        try (InputStream in = zip.getInputStream(entry)) {
                            FileUtils.copyFile(in, new File(target, entry.getName()));
                        }
                    }
                }
            }
        }
    }

    private static void extractFeaturePackContents(FeaturePack featurePack, File outputDirectory, Set<String> filesProcessed) throws IOException {
        final int fileNameWithoutContentsStart = Locations.CONTENT.length() + 1;
        try(JarFile jar = new JarFile(featurePack.getFeaturePackFile())) {
            for (String contentFile : featurePack.getContentFiles()) {
                final String outputFile = contentFile.substring(fileNameWithoutContentsStart);
                if (!filesProcessed.add(outputFile)) {
                    continue;
                }
                FileUtils.extractFile(jar, contentFile, new java.io.File(outputDirectory, outputFile));
            }
        }
        for (FeaturePack dependency : featurePack.getDependencies()) {
            extractFeaturePackContents(dependency, outputDirectory, filesProcessed);
        }
    }

    private static void processFilePermissions(FeaturePack featurePack, File outputDirectory) throws IOException {
        final Path baseDir = Paths.get(outputDirectory.getAbsolutePath());
        final List<FilePermission> filePermissions = featurePack.getDescription().getFilePermissions();
        Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String relative = baseDir.relativize(dir).toString();
                if (!OS_WINDOWS) {
                    for (FilePermission perm : filePermissions) {
                        if (perm.includeFile(relative)) {
                            Files.setPosixFilePermissions(dir, perm.getPermission());
                            continue;
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relative = baseDir.relativize(file).toString();
                if (!OS_WINDOWS) {
                    for (FilePermission perm : filePermissions) {
                        if (perm.includeFile(relative)) {
                            Files.setPosixFilePermissions(file, perm.getPermission());
                            continue;
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        for (FeaturePack dependency : featurePack.getDependencies()) {
            processFilePermissions(dependency, outputDirectory);
        }
    }

    static Logger getLog() {
        return logger;
    }

}
