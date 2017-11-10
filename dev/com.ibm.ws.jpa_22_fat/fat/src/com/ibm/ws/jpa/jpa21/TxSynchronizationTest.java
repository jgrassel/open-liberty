/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.jpa21;

import java.io.File;
import java.util.HashSet;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.JPAFATServletClient;
import com.ibm.ws.jpa.jpa10.CallbackTest;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;
import txsync.web.TxSyncEJBTestServlet;
import txsync.web.TxSyncTestServlet;

@RunWith(FATRunner.class)
public class TxSynchronizationTest extends JPAFATServletClient {
    private static final String TESTAPP_ROOT = "test-applications/jpa21/TxSynchronization";
    private static final String PKG_ROOT = "txsync";
    private static final String resPath = TESTAPP_ROOT + "/resources/";

    public static final String APP_NAME = "TxSynchronization";
    public static final String SERVLET = "TxSyncTestServlet";
    public static final String EJBSERVLET = "TxSyncEJBTestServlet";

//    private final static Set<String> dropSet = new HashSet<String>();
//    private final static Set<String> createSet = new HashSet<String>();
//
    private static long timestart = 0;

//    static {
//        dropSet.add("JPA21_TXSYNC_DROP_${dbvendor}.ddl");
//        createSet.add("JPA21_TXSYNC_CREATE_${dbvendor}.ddl");
//    }

    @Server("JPATxSynchFATServer")
    @TestServlets({
                    @TestServlet(servlet = TxSyncTestServlet.class, path = APP_NAME + "/" + SERVLET),
                    @TestServlet(servlet = TxSyncEJBTestServlet.class, path = APP_NAME + "/" + EJBSERVLET),
    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, PrivHelper.JAXB_PERMISSION);
        bannerStart(CallbackTest.class);
        timestart = System.currentTimeMillis();

        server1.startServer();

//        setupDatabaseApplication(server1, resPath + "/ddl/");
//
//        final Set<String> ddlSet = new HashSet<String>();
//
//        ddlSet.clear();
//        for (String ddlName : dropSet) {
//            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
//        }
//        executeDDL(server1, ddlSet, true);
//
//        ddlSet.clear();
//        for (String ddlName : createSet) {
//            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
//        }
//        executeDDL(server1, ddlSet, false);

        setupTestApplication();
    }

    private static void setupTestApplication() throws Exception {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, APP_NAME + ".jar");
        ejbJar.addPackage(PKG_ROOT + ".ejb");
        ejbJar.addPackage(PKG_ROOT + ".ejblocal");
        ejbJar.addPackage(PKG_ROOT + ".logic");
        ejbJar.addPackage(PKG_ROOT + ".model");
        ShrinkHelper.addDirectory(ejbJar, resPath + "/ejb");

        final WebArchive webapp = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        webapp.addPackage(PKG_ROOT + ".ejblocal");
        webapp.addPackage(PKG_ROOT + ".logic");
        webapp.addPackage(PKG_ROOT + ".model");
        webapp.addPackage(PKG_ROOT + ".web");
        ShrinkHelper.addDirectory(webapp, resPath + "/webapp");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        app.addAsModule(ejbJar);
        app.addAsModule(webapp);
        app.setApplicationXML(new File(resPath + "/ear/META-INF/application.xml"));

        ShrinkHelper.exportAppToServer(server1, app);

        Application appRecord = new Application();
        appRecord.setLocation(APP_NAME + ".ear");
        appRecord.setName(APP_NAME);

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().clear();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(APP_NAME);
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWJP9991W");
    }
}
