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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import org.junit.Assert;

public class DerbyNetworkServerControl {
    private static URLClassLoader derbyCL = null;
    private static Class<?> derbyNSCClass = null; // org.apache.derby.drda.NetworkServerControl
    private static Constructor<?> derbyNSCtor = null; // (InetAddress, int)
    private static Method derbyNSMethod_START = null; // (PrintWriter)
    private static Method derbyNSMethod_STOP = null; // ()
    private static Method derbyNSMethod_PING = null; // ()

    private static Object derbyNSInstance = null;
    private static boolean derbyNSStarted = false;

    private static InetAddress derbyHost = null;
    private static int derbyPortNumber = -1;
    private static final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private static final PrintWriter derbyNSPW = new PrintWriter(baos);

    private static final int SECONDS_TO_START = 60;
    private static final int SECONDS_TO_STOP = 60;

    private static void setUp() throws Exception {
        // Find Derby Jars in autoFVT/publish/shared/resources/derby
        ArrayList<URL> urlList = new ArrayList<URL>();
        Path derbyPath = Paths.get(System.getProperty("user.dir")).resolve("publish/shared/resources/derby");
        Files.walkFileTree(derbyPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                final String fileName = file.getFileName().toString();
                if (fileName.equals("derby.jar") || fileName.equals("derbynet.jar") || fileName.equals("derbyclient.jar")) {
                    urlList.add(file.toUri().toURL());
                }

                return FileVisitResult.CONTINUE;
            }
        });

        Assert.assertEquals(3, urlList.size());
        URL[] urlArr = new URL[urlList.size()];
        int idx = 0;
        for (URL url : urlList) {
            urlArr[idx++] = url;
        }
        derbyCL = new URLClassLoader(urlArr);
        derbyNSCClass = derbyCL.loadClass("org.apache.derby.drda.NetworkServerControl");

        derbyNSCtor = derbyNSCClass.getConstructor(java.net.InetAddress.class, int.class);
        derbyNSMethod_START = derbyNSCClass.getMethod("start", java.io.PrintWriter.class);
        derbyNSMethod_STOP = derbyNSCClass.getMethod("shutdown");
        derbyNSMethod_PING = derbyNSCClass.getMethod("ping");
    }

    public static int getDerbyPortNumber() {
        return derbyPortNumber;
    }

    public static String getDerbyHostName() {
        if (derbyHost == null) {
            return null;
        }
        return derbyHost.getHostName();
    }

    public static String getDerbyHostAddress() {
        if (derbyHost == null) {
            return null;
        }
        return derbyHost.getHostAddress();
    }

    public static InetAddress getDerbyHostInetAddress() {
        return derbyHost;
    }

    public static void startDerbyNetworkServer() throws Exception {
        if (derbyCL == null) {
            setUp();
        }

        if (derbyNSInstance != null && derbyNSStarted) {
            throw new IllegalStateException("Already active Derby Network Server at " + derbyNSInstance);
        }

        derbyHost = InetAddress.getLocalHost();

        System.out.println("Attempting to start Derby Network Server ...");
        // If starting derby network server for the first time, we must discover an available port.
        // Once discovered, we can reuse that port for the remainder of the test.
        if (derbyPortNumber == -1) {
            // Start at port 1500, increment up to 65535 if port isn't available.
            for (int portNum = 1500; portNum <= 65535; portNum++) {
                try {
                    derbyNSInstance = derbyNSCtor.newInstance(derbyHost, portNum);
                    derbyNSMethod_START.invoke(derbyNSInstance, derbyNSPW);
                    Assert.assertNotNull(derbyNSInstance);

                    // Found a port that the server can bind to, record the port and wait until the
                    // server is actually accepting connections.
                    derbyPortNumber = portNum;
                    System.out.println("Derby Network Server binding to port " + derbyPortNumber);
                    waitUntilDerbyServerIsActuallyListening();

                    System.out.println("Derby Network Server started at port " + derbyPortNumber);
                    derbyNSStarted = true;
                    return;
                } catch (IllegalAccessException | IllegalArgumentException iae) {
                    throw new Exception(iae);
                } catch (InvocationTargetException e) {
                    // Exception starting the server, likely port is already in use.  Try another.
                    e.printStackTrace();
                }
            }

            throw new Exception("Could not start Derby Network Server -- no available ports.");
        } else {
            System.out.println("Attempting to start Derby Network Server at port " + derbyPortNumber + " ...");
            try {
                derbyNSInstance = derbyNSCtor.newInstance(InetAddress.getLocalHost(), derbyPortNumber);
                Assert.assertNotNull(derbyNSInstance);
                derbyNSMethod_START.invoke(derbyNSInstance, derbyNSPW);

                waitUntilDerbyServerIsActuallyListening();

                derbyNSStarted = true;
                return;
            } catch (IllegalAccessException | IllegalArgumentException iae) {
                throw new Exception(iae);
            } catch (InvocationTargetException e) {
                throw new Exception(e.getTargetException());
            }
        }
    }

    public static void stopDerbyNetworkServer() throws Exception {
        if (derbyCL == null) {
            setUp();
        }

        if (derbyNSInstance == null || !derbyNSStarted) {
            throw new IllegalStateException("No active Derby Network Server at " + derbyNSInstance);
        }

        System.out.println("Stopping Derby Network Server ...");
        try {
            derbyNSMethod_STOP.invoke(derbyNSInstance);
            derbyNSStarted = false;
            waitUntilDerbyServerIsActuallyStopped();
            System.out.println("Derby Network Server has stopped.");
        } catch (IllegalAccessException | IllegalArgumentException iae) {
            throw new Exception(iae);
        } catch (InvocationTargetException e) {
            throw new Exception(e.getTargetException());
        }
    }

    public static boolean isServerUp() {
        if (derbyCL == null) {
            try {
                setUp();
            } catch (Throwable t) {
                return false;
            }
        }

        if (derbyNSInstance == null || !derbyNSStarted) {
            return false;
        }

        try {
            derbyNSMethod_PING.invoke(derbyNSInstance);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void waitUntilDerbyServerIsActuallyListening() {
        if (derbyCL == null || derbyNSInstance == null) {
            throw new IllegalStateException("Derby Server has not been initialized.");
        } else if (derbyPortNumber == -1 || derbyHost == null) {
            throw new IllegalStateException("Derby Server has not been started.");
        }

        long startTime = System.currentTimeMillis();
        long expireTime = startTime + (1000 * SECONDS_TO_START);

        System.out.println("Verifying that Derby Network Server is listening to port " + derbyPortNumber);
        while (System.currentTimeMillis() <= expireTime) {
            Socket sock = null;
            try {
                sock = new Socket(derbyHost, derbyPortNumber);
                long endTime = System.currentTimeMillis();
                System.out.println("Server successfully started in " + ((endTime - startTime) / 1000) + "s");
                // Socket successfully connected, so Derby NS is now listening.
                return;
            } catch (IOException e) {
                // Server isn't actually listening yet.
            } finally {
                if (sock != null) {
                    try {
                        sock.close();
                    } catch (Throwable t) {
                        // Swallow
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    // Swallow
                }
            }
        }

        long endTime = System.currentTimeMillis();
        throw new IllegalStateException("Server has not started in the allowable elapse time ("
                                        + ((endTime - startTime) / 1000) + "s)");
    }

    private static void waitUntilDerbyServerIsActuallyStopped() {
        if (derbyCL == null || derbyNSInstance == null) {
            throw new IllegalStateException("Derby Server has not been initialized.");
        } else if (derbyPortNumber == -1 || derbyHost == null) {
            return;
        }

        long startTime = System.currentTimeMillis();
        long expireTime = startTime + (1000 * SECONDS_TO_STOP);

        System.out.println("Waiting for Derby Network Server listening to port " + derbyPortNumber + " to stop...");
        while (System.currentTimeMillis() <= expireTime) {
            Socket sock = null;
            try {
                sock = new Socket(derbyHost, derbyPortNumber);
                // Still listening to the port.
            } catch (IOException e) {
                // Server has stopped.
                long endTime = System.currentTimeMillis();
                System.out.println("Server successfully stopped after " + ((endTime - startTime) / 1000) + "s");
                // Socket successfully connected, so Derby NS is now listening.
                return;
            } finally {
                if (sock != null) {
                    try {
                        sock.close();
                    } catch (Throwable t) {
                        // Swallow
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    // Swallow
                }
            }
        }

        long endTime = System.currentTimeMillis();
        throw new IllegalStateException("Server has not stopped in the allowable elapse time ("
                                        + ((endTime - startTime) / 1000) + "s)");
    }
}
