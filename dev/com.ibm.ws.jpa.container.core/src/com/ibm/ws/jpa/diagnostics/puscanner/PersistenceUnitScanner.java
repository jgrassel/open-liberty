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

package com.ibm.ws.jpa.diagnostics.puscanner;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

import javax.persistence.spi.PersistenceUnitInfo;

import com.ibm.ws.jpa.diagnostics.class_scanner.ano.ClassScannerException;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.EntityMappingsScanner;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.EntityMappingsScannerResults;
import com.ibm.ws.jpa.diagnostics.ormparser.EntityMappingsDefinition;
import com.ibm.ws.jpa.diagnostics.ormparser.EntityMappingsException;
import com.ibm.ws.jpa.diagnostics.ormparser.EntityMappingsFactory;

public final class PersistenceUnitScanner {
    public static PersistenceUnitScannerResults scanPersistenceUnit(PersistenceUnitInfo pUnit) 
            throws PersistenceUnitScannerException {
        if (pUnit == null) {
            throw new PersistenceUnitScannerException("Cannot accept a null value for PersistenceUnitInfo argument.");
        }
        
        PersistenceUnitScanner puScanner = new PersistenceUnitScanner(pUnit);
        return puScanner.scan();
    }

    final private PersistenceUnitInfo pUnit;
    final private ClassLoader tempCL;
    
    final private URL puRoot;
    final private List<URL> jarFileList = new ArrayList<URL>();
    
    // Scanner Result Containers
    final List<EntityMappingsDefinition> entityMappingsDefinitionsList = new ArrayList<EntityMappingsDefinition>();
    final List<EntityMappingsScannerResults> classScannerResults = new ArrayList<EntityMappingsScannerResults>();
    
    
    private PersistenceUnitScanner(PersistenceUnitInfo pUnit) {
        this.pUnit = pUnit;
        this.tempCL = pUnit.getNewTempClassLoader();
        
        this.puRoot = pUnit.getPersistenceUnitRootUrl();
        if (pUnit.getJarFileUrls() != null) {
            jarFileList.addAll(pUnit.getJarFileUrls());
        }       
    }
    
    private PersistenceUnitScannerResults scan() throws PersistenceUnitScannerException {
        scanEntityMappings();
        scanClasses();
        
        return new PersistenceUnitScannerResults(pUnit, entityMappingsDefinitionsList, classScannerResults);
    }
    
    /**
     * Scan Entity Mappings Files
     */
    private void scanEntityMappings() throws PersistenceUnitScannerException {
        /*
         * From the JPA 2.1 Specification:
         * 
         * 8.2.1.6.2 Object/relational Mapping Files
         * An object/relational mapping XML file contains mapping information for the classes listed in it.
         * 
         * A object/relational mapping XML file named orm.xml may be specified in the META-INF directory
         * in the root of the persistence unit or in the META-INF directory of any jar file referenced by the persistence.
         * xml. Alternatively, or in addition, one or more mapping files may be referenced by the
         * mapping-file elements of the persistence-unit element. These mapping files may be
         * present anywhere on the class path.
         * 
         * An orm.xml mapping file or other mapping file is loaded as a resource by the persistence provider. If
         * a mapping file is specified, the classes and mapping information specified in the mapping file will be
         * used as described in Chapter 12. If multiple mapping files are specified (possibly including one or more
         * orm.xml files), the resulting mappings are obtained by combining the mappings from all of the files.
         * The result is undefined if multiple mapping files (including any orm.xml file) referenced within a single
         * persistence unit contain overlapping mapping information for any given class. The object/relational
         * mapping information contained in any mapping file referenced within the persistence unit must be disjoint
         * at the class-level from object/relational mapping information contained in any other such mapping
         * file.
         */
        final HashSet<URL> mappingFilesLocated = new HashSet<URL>();
        
        try {
            // Search for the default "orm.xml" in the persistence unit root and jar files
            mappingFilesLocated.addAll(findORMResources("META-INF/orm.xml"));
            
            // Search for all other declared mapping files
            final List<String> puiMappingFiles = pUnit.getMappingFileNames();
            if (puiMappingFiles != null && !puiMappingFiles.isEmpty()) {
                for (String mappingFile : puiMappingFiles) {
                    if ("META-INF/orm.xml".equals(mappingFile)) {
                        continue; // Skip, already processed the default orm.xml
                    }
                    mappingFilesLocated.addAll(findORMResources(mappingFile));
                }
            }
            
            // Process discovered mapping files       
            for (final URL mappingFileURL : mappingFilesLocated) {
                EntityMappingsDefinition emapdef = EntityMappingsFactory.parseEntityMappings(mappingFileURL);
                entityMappingsDefinitionsList.add(emapdef);
            }   
        } catch (IOException ioe) {
            throw new PersistenceUnitScannerException(ioe);
        } catch (EntityMappingsException eme) {
            throw new PersistenceUnitScannerException(eme);
        }               
    }
    
    /**
     * Scan classes in persistence unit root and referenced jar-files 
     */
    private void scanClasses() throws PersistenceUnitScannerException {
        
        // Persistence Unit Root
        try {
            // Persistence Unit Root
            classScannerResults.add(EntityMappingsScanner.scanTargetArchive(puRoot, tempCL));
            
            // Listed Jar Files
            for (final URL jarFileURL : jarFileList) {
                classScannerResults.add(EntityMappingsScanner.scanTargetArchive(jarFileURL, tempCL));
            }
        } catch (ClassScannerException cse) {
            throw new PersistenceUnitScannerException(cse);
        }
    }
    
    /**
     * Finds all specified ORM files, by name, constrained in location by the persistence unit root and jar files.
     * 
     * @param ormFileName The name of the ORM file to search for
     * @return A List of URLs of resources found by the ClassLoader.  Will be an empty List if none are found.
     * @throws IOException
     */
    private List<URL> findORMResources(String ormFileName) throws IOException {
        boolean isMetaInfoOrmXML = "META-INF/orm.xml".equals(ormFileName);
        
        final ArrayList<URL> retArr = new ArrayList<URL>();
        
        Enumeration<URL> ormEnum = pUnit.getClassLoader().getResources(ormFileName);
        while (ormEnum.hasMoreElements()) {
            final URL url = ormEnum.nextElement();
            final String urlExtern = url.toExternalForm(); //  ParserUtils.decode(url.toExternalForm());
            
            if (!isMetaInfoOrmXML) {
                // If it's not "META-INF/orm.xml", then the mapping files may be present anywhere in the classpath.
                retArr.add(url);
                continue;
            }
            
            // Check against persistence unit root
            if (urlExtern.startsWith(puRoot.toExternalForm())) {
                retArr.add(url);
                continue;
            }
            
            // Check against Jar files, if any
            for (URL jarUrl : jarFileList) {
                final String jarExtern = jarUrl.toExternalForm();
                if (urlExtern.startsWith(jarExtern)) {
                    retArr.add(url);
                    continue;
                }
            }
        }       
        
        return retArr;
    }   
}
