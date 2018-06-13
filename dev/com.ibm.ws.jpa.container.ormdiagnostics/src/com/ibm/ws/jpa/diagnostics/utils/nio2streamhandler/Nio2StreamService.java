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

package com.ibm.ws.jpa.diagnostics.utils.nio2streamhandler;

import java.net.URL;
import java.nio.file.FileSystem;

public final class Nio2StreamService {
    private static boolean hasRegistered = false;

    public static synchronized void registerNio2StreamService() {
        if (!hasRegistered) {
            URL.setURLStreamHandlerFactory(new Nio2FileSystemURLStreamHandlerFactory());
            hasRegistered = true;
        }
    }

    public static synchronized Nio2FileSystemRegistryToken registerFileSystem(FileSystem fs) {
        registerNio2StreamService();
        if (fs == null) {
            throw new NullPointerException();
        }
        return Nio2StreamServiceRegistry.registerFileSystem(fs);
    }

    public static synchronized boolean deregisterFileSystem(Nio2FileSystemRegistryToken token) {
        registerNio2StreamService();
        if (token == null) {
            throw new NullPointerException();
        }
        token.deregister();
        return Nio2StreamServiceRegistry.deregisterFileSystem(token);
    }
}
