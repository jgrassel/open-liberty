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

import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Nio2StreamServiceRegistry {
    private static final HashMap<UUID, Nio2FileSystemRegistryToken> tokenMap = new HashMap<UUID, Nio2FileSystemRegistryToken>();

    private Nio2StreamServiceRegistry() {
        // No instances of this class are needed
    }

    /**
     * Registers the FileSystem under the specified identifier, as long as the identifier is not already used.
     * 
     * @param fs - FileSystem to register
     * @return - Returns a Nio2FileSystemRegistryToken associated with the FileSystem.  If the FileSystem was
     * already registered, then the original entry is returned.  Otherwise a new entry is returned.
     */
    synchronized static Nio2FileSystemRegistryToken registerFileSystem(FileSystem fs) {
        if (fs == null) {
            throw new NullPointerException("Method registerFileSystem cannot accept null arguments.");
        }
        
        if (!fs.isOpen()) {
            throw new IllegalStateException("FileSystem must be open.");
        }
        
        // Check if FileSystem is already registered
        for (Map.Entry<UUID, Nio2FileSystemRegistryToken> entry : tokenMap.entrySet()) {
            if (entry.getValue().getFileSystem().equals(fs)) {
                return entry.getValue();
            }
        }
               
        final UUID newid = UUID.randomUUID();
        final Nio2FileSystemRegistryToken newToken = new Nio2FileSystemRegistryToken(newid, fs);
        tokenMap.put(newid, newToken);
        
        return newToken;
    }
    
    /**
     * Deregisters the specified FileSystem.
     * 
     * @param token - FileSystem referenced by Nio2FileSystemRegistryToken to deregister
     * @return - Returns true if the FileSystem was deregistered, false if it was not in the registry.
     */
    synchronized static boolean deregisterFileSystem(Nio2FileSystemRegistryToken token) {   
        if (token == null) {
            return false;
        }
        
        return (tokenMap.remove(token.getUuid()) != null);
    }
    
    synchronized static Nio2FileSystemRegistryToken resolve(UUID uuid) {
        return tokenMap.get(uuid);
    }
    
}
