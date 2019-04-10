/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa;

import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.PrivHelper;
import jpabootstrap.web.TestJPABootstrapServlet;

@RunWith(FATRunner.class)
public class EMFStartupFailureRecoveryTest extends FATServletClient {
    public static final String APP_NAME = "jpabootstrap";
    public static final String SERVLET = "TestJPABootstrap";

    @Server("EMFFailoverOnAppStartServer")
    @TestServlets({
                    @TestServlet(servlet = TestJPABootstrapServlet.class, path = APP_NAME + "_2.2/" + SERVLET)
    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        createApplication("2.2");
    }

    private static void createApplication(String specLevel) throws Exception {
        final String resPath = "test-applications/" + APP_NAME + "/resources/jpa-" + specLevel + "/";

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + "_" + specLevel + ".war");
        app.addPackage("jpabootstrap.web");
        app.addPackage("jpabootstrap.entity");
        app.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class).importDirectory(resPath).as(GenericArchive.class),
                  "/",
                  Filters.includeAll());
        ShrinkHelper.exportDropinAppToServer(server1, app);
    }

    /**
     * Test for validating Derby Network Server Unit Test Control. Do not enable for regular
     * FAT execution.
     */
//    @Test
    public void testDerbyNetworkServerSetup() throws Exception {
        Assert.assertFalse(DerbyNetworkServerControl.isServerUp());

        DerbyNetworkServerControl.startDerbyNetworkServer();
        Assert.assertTrue(-1 != DerbyNetworkServerControl.getDerbyPortNumber());
        Assert.assertTrue(DerbyNetworkServerControl.isServerUp());
        System.out.println("Derby network server listening to port " + DerbyNetworkServerControl.getDerbyPortNumber());

        try {
            DerbyNetworkServerControl.startDerbyNetworkServer();
            Assert.fail("Failed to throw IllegalStateException trying to start second instance.");
        } catch (IllegalStateException iae) {
            // Expected
        }

        DerbyNetworkServerControl.stopDerbyNetworkServer();
        Assert.assertFalse(DerbyNetworkServerControl.isServerUp());
    }

    @Test
    public void testEMFFailoverOnAppStart() throws Exception {
        DerbyNetworkServerControl.startDerbyNetworkServer();
        Assert.assertTrue(DerbyNetworkServerControl.isServerUp());
        int derbyPort = DerbyNetworkServerControl.getDerbyPortNumber();
        Assert.assertTrue(-1 != derbyPort);

        server1.addEnvVar("derbyserver.hostname", DerbyNetworkServerControl.getDerbyHostAddress());
        server1.addEnvVar("derbyserver.port", Integer.toString(derbyPort));
        server1.saveServerConfiguration();
        server1.startServer();

        runTest("2.2");

        server1.stopServer("CWWJP9991W:");

        DerbyNetworkServerControl.stopDerbyNetworkServer();
    }

    private void runTest(String spec) throws Exception {
        FATServletClient.runTest(server1, APP_NAME + "_" + spec + "/TestJPABootstrap", "testPersistenceUnitBootstrap");

    }
}
