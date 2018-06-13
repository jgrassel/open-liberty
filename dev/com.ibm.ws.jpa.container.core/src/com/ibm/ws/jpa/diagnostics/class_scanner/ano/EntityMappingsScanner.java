/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.class_scanner.ano;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInformationType;

public final class EntityMappingsScanner {
    public static EntityMappingsScannerResults scanTargetArchive(URL targetArchive, ClassLoader scannerCL)
            throws ClassScannerException {
        if (targetArchive == null || scannerCL == null) {
            throw new ClassScannerException("EntityMappingsScanner.scanTargetArchive cannot accept null arguments.");
        }

        EntityMappingsScanner ems = new EntityMappingsScanner(targetArchive, scannerCL);
        ClassInformationType cit = ems.scanTargetArchive();
        return new EntityMappingsScannerResults(cit, targetArchive);
    }
    
    private final URL targetArchive;
    private final ClassLoader scannerCL;
    private final InnerOuterResolver ioResolver = new InnerOuterResolver();

    private EntityMappingsScanner(URL targetArchive, ClassLoader scannerCL) {
        this.targetArchive = targetArchive;
        this.scannerCL = scannerCL;
    }

    private ClassInformationType scanTargetArchive() throws ClassScannerException {
        final HashSet<ClassInfoType> citSet = new HashSet<ClassInfoType>();

        /*
         * The JPA Specification's PersistenceUnitInfo contract for getJarFileURLs() and
         * getPersistenceUnitRoot() makes the following mandate:
         * 
         * A URL will either be a file: URL referring to a jar file or referring to a
         * directory that contains an exploded jar file, or some other URL from which an
         * InputStream in jar format can be obtained.
         */
        final String urlProtocol = targetArchive.getProtocol();
        if ("file".equalsIgnoreCase(urlProtocol)) {
            // Protocol is "file", which either addresses a jar file or an exploded jar file
            try {
                Path taPath = Paths.get(targetArchive.toURI());
                if (Files.isDirectory(taPath)) {
                    // Exploded Archive
                    citSet.addAll(processExplodedJarFormat(taPath));
                } else {
                    // Unexploded Archive
                    System.err.println("Unexploded Archive Path");
                }

            } catch (URISyntaxException e) {
                throw new ClassScannerException(e);
            }

        } else {
            // InputStream will be in jar format.
            citSet.addAll(processJarFormatInputStreamURL(targetArchive));
        }

        // // Scan the classes found in the referenced archive
        // final Set<ClassInfoType> scannedClasses = new HashSet<ClassInfoType>();
        // for (final String className : classNames) {
        // try {
        // final Class<?> targetClass = scannerCL.loadClass(className);
        // scannedClasses.add(AsmClassAnalyzer.analyzeClass(targetClass));
        // } catch (ClassNotFoundException e) {
        // // TODO: Properly log
        // e.printStackTrace();
        // }
        // }

        ClassInformationType cit = new ClassInformationType();
        List<ClassInfoType> citList = cit.getClassInfo();
        citList.addAll(citSet);
        
        ioResolver.resolve(citList);

        return cit;
    }

    private Set<ClassInfoType> processExplodedJarFormat(Path path) throws ClassScannerException {
        final HashSet<ClassInfoType> citSet = new HashSet<ClassInfoType>();
        final HashSet<Path> archiveFiles = new HashSet<Path>();

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (Files.isRegularFile(file) && Files.size(file) > 0
                            && file.getFileName().toString().endsWith(".class")) {
                        archiveFiles.add(file);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            for (Path p : archiveFiles) {
                String cName = path.relativize(p).toString().replace("/", ".");
                cName = cName.substring(0, cName.length() - 6); // Remove ".class" from name

                try (InputStream is = Files.newInputStream(p)) {
                    citSet.add(scanByteCodeFromInputStream(cName, is));
                } catch (Throwable t) {
                    throw new ClassScannerException(t);
                }
            }
        } catch (ClassScannerException cse) {
            throw cse;
        } catch (Throwable t) {
            throw new ClassScannerException(t);
        }

        return citSet;
    }

    private Set<ClassInfoType> processJarFormatInputStreamURL(URL jarURL) throws ClassScannerException {
        final HashSet<ClassInfoType> citSet = new HashSet<ClassInfoType>();

        try (JarInputStream jis = new JarInputStream(jarURL.openStream(), false)) {
            JarEntry jarEntry = null;
            while ((jarEntry = jis.getNextJarEntry()) != null) {
                String name = jarEntry.getName();
                if (name != null && name.endsWith(".class")) {
                    name = name.substring(0, name.length() - 6).replace("/", ".");
                    citSet.add(scanByteCodeFromInputStream(name, jis));
                }
            }
        } catch (Throwable t) {
            throw new ClassScannerException(t);
        }

        return citSet;
    }

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final byte[] buffer = new byte[4096];

    private ClassInfoType scanByteCodeFromInputStream(String cName, InputStream is) throws ClassScannerException {
        baos.reset();

        try {
            int bytesRead = 0;
            while ((bytesRead = is.read(buffer, 0, 4096)) > -1) {
                if (bytesRead > 0) {
                    baos.write(buffer, 0, bytesRead);
                }
            }

            byte[] classByteCode = baos.toByteArray();
            baos.reset();

            return AsmClassAnalyzer.analyzeClass(cName, classByteCode, ioResolver);
        } catch (Throwable t) {
            throw new ClassScannerException(t);
        }
    }
}
