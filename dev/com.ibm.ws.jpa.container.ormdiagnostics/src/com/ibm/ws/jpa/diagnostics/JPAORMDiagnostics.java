/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.diagnostics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.persistence.spi.PersistenceUnitInfo;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.EntityMappingsScannerResults;
import com.ibm.ws.jpa.diagnostics.ormparser.EntityMappingsDefinition;
import com.ibm.ws.jpa.diagnostics.ormparser.entitymapping.IEntityMappings;
import com.ibm.ws.jpa.diagnostics.puscanner.PersistenceUnitScanner;
import com.ibm.ws.jpa.diagnostics.puscanner.PersistenceUnitScannerResults;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.EncapsulatedData;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.EncapsulatedDataGroup;

/**
 *
 */
public class JPAORMDiagnostics {
    private static final TraceComponent tc = Tr.register(JPAORMDiagnostics.class,
                                                         "JPA",
                                                         "com.ibm.ws.jpa.jpa");

    public static void generateJPAORMDiagnostics(PersistenceUnitInfo pui, InputStream pxmlIS) {
        try {
            if (pui == null || !(tc.isAnyTracingEnabled() && tc.isDebugEnabled())) {
                return;
            }

            final PersistenceUnitScannerResults pusr = PersistenceUnitScanner.scanPersistenceUnit(pui);
            final String puName = pusr.getPersistenceUnitName();
            final List<EntityMappingsScannerResults> clsScanResultsList = pusr.getClassScannerResults();
            final List<EntityMappingsDefinition> entityMappingDefList = pusr.getEntityMappingsDefinitionsList();

            EncapsulatedDataGroup edg = EncapsulatedDataGroup.createEncapsulatedDataGroup("ORMDiagnostics", "ORMDiagnostics");
            int id = 0;

            // Set Properties
            edg.setProperty("execution.environment", "WebSphere Liberty");
            edg.setProperty("Persistence Unit Name", puName);

            List<URL> jpaFileURLList = pui.getJarFileUrls();
            int jarUrlCount = 0;
            if (jpaFileURLList != null && jpaFileURLList.size() > 0) {
                for (URL url : jpaFileURLList) {
                    edg.setProperty("jar_file_" + ++jarUrlCount, url.toString());
                }
            }

            edg.putDataItem(EncapsulatedData.createEncapsulatedData("persistence.xml", Integer.toString(id++), readPXmlInputStream(pxmlIS)));

            EncapsulatedDataGroup edgClassScanner = EncapsulatedDataGroup.createEncapsulatedDataGroup("ClassScanner", "ClassScanner");
            edg.putDataSubGroup(edgClassScanner);
            for (EntityMappingsScannerResults emsr : clsScanResultsList) {
                System.out.println(emsr);
                byte[] data = emsr.produceXML();
                String name = emsr.getTargetArchive().toString();
                String idStr = Integer.toString(id++);
                EncapsulatedData ed = EncapsulatedData.createEncapsulatedData(name, idStr, data);
                edgClassScanner.putDataItem(ed);
            }

            EncapsulatedDataGroup edgEntityMappings = EncapsulatedDataGroup.createEncapsulatedDataGroup("EntityMappings", "EntityMappings");
            edg.putDataSubGroup(edgEntityMappings);
            for (EntityMappingsDefinition emd : entityMappingDefList) {
                IEntityMappings em = emd.getEntityMappings();
                String name = emd.getSource().toString();
                String idStr = Integer.toString(id++);
                byte[] ormXmlData = emd.getEntityMappingsAsXMLBytes();
                EncapsulatedData ed = EncapsulatedData.createEncapsulatedData(name, idStr, ormXmlData);
                edgEntityMappings.putDataItem(ed);
            }

            final StringBuilder ormDiagText = new StringBuilder();
            ormDiagText.append("##### BEGIN JPA ORM Diagnostics\n");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            edg.writeToString(baos);
            ormDiagText.append(baos.toString());
            ormDiagText.append("\n##### END JPA ORM Diagnostics");

            Tr.debug(tc, "JPAORMDiagnostics Dump", ormDiagText.toString());
        } catch (Throwable t) {
            // Swallow any exceptions that are caused by the ORM Diagnostic Reporter.  If it bombs,
            // it must not be permitted to affect application start.
        }
    }

    private static byte[] readPXmlInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = -1;

        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        is.close();

        return baos.toByteArray();
    }
}
