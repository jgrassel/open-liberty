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

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.persistence.spi.PersistenceUnitInfo;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.EntityMappingsScannerResults;
import com.ibm.ws.jpa.diagnostics.ormparser.EntityMappingsDefinition;
import com.ibm.ws.jpa.diagnostics.puscanner.PersistenceUnitScanner;
import com.ibm.ws.jpa.diagnostics.puscanner.PersistenceUnitScannerResults;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.EncapsulatedData;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.EncapsulatedDataGroup;
import com.ibm.ws.jpa.management.AbstractJPAComponent;

/**
 *
 */
public class JPAORMDiagnostics {
    private static final TraceComponent tc = Tr.register(AbstractJPAComponent.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    public static void generateJPAORMDiagnostics(PersistenceUnitInfo pui) {
        try {
            if (pui == null || !(tc.isAnyTracingEnabled() && tc.isDebugEnabled())) {
                return;
            }

            final PersistenceUnitScannerResults pusr = PersistenceUnitScanner.scanPersistenceUnit(pui);
            final String puName = pusr.getPersistenceUnitName();
            final List<EntityMappingsScannerResults> clsScanResultsList = pusr.getClassScannerResults();
            final List<EntityMappingsDefinition> entityMappingDefList = pusr.getEntityMappingsDefinitionsList();

            EncapsulatedDataGroup edg = EncapsulatedDataGroup.createEncapsulatedDataGroup("ORMDiagnostics", "ORMDiagnostics");
            edg.setProperty("Persistence Unit Name", puName);

            int id = 0;

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
//            for (EntityMappingsDefinition emd : entityMappingDefList) {
////                byte[] data = emd.
//            }

            final StringBuilder ormDiagText = new StringBuilder();
            ormDiagText.append("\n##### BEGIN JPA ORM Diagnostics\n");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            edg.writeToString(baos);
            ormDiagText.append(baos.toString());
            ormDiagText.append("\n##### END JPA ORM Diagnostics");

            Tr.debug(tc, "JPAORMDiag", ormDiagText.toString());
        } catch (Throwable t) {
            // Swallow any exceptions that are caused by the ORM Diagnostic Reporter.  If it bombs,
            // it must not be permitted to affect application start.
        }
    }
}
