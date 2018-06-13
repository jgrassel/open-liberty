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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.UUID;

/**
 * A token returned by the Nio2FileServiceRegistry with which a user can build URLs with to reference content
 * within the associated FileSystem.
 * 
 *
 */
public class Nio2FileSystemRegistryToken {
    private UUID uuid;
    private FileSystem fileSystem;
    private boolean registered = true;

    Nio2FileSystemRegistryToken(UUID uuid, FileSystem fileSystem) {
        if (uuid == null || fileSystem == null) {
            throw new NullPointerException("Nio2FileSystemRegistryToken ctor cannot accept null arguments.");
        }
        this.uuid = uuid;
        this.fileSystem = fileSystem;
    }

    public final UUID getUuid() {
        return uuid;
    }

    public final FileSystem getFileSystem() {
        return fileSystem;
    }

    public boolean isRegistered() {
        return registered;
    }
    
    void deregister() {
        registered = false;
    }
    
    @Override
    public int hashCode() {
        return fileSystem.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        Nio2FileSystemRegistryToken other = (Nio2FileSystemRegistryToken) obj;
        if (fileSystem == null) {
            if (other.fileSystem != null) {
                return false;
            }
        } else if (!fileSystem.equals(other.fileSystem)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Nio2FileSystemRegistryToken [uuid=" + uuid + ", fileSystem=" + fileSystem + ", registered=" + 
                registered + "]";
    }
    
    public URL url(String path, boolean jarFileFormat) throws MalformedURLException {
        Path nioPath = fileSystem.getPath(path);
        return url(nioPath, jarFileFormat);
    }
    
    public URL url(Path path, boolean jarFileFormat) throws MalformedURLException {
        if (!isRegistered()) {
            throw new IllegalStateException("Cannot create a new URL with a deregistered Nio2FileSystemRegistryToken.");
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(Nio2FileSystemURLStreamHandlerFactory.PROTOCOL).append(":"); // Protocol
        sb.append("//").append(this.uuid).append("/"); // Registered FileSystem identifier
        sb.append(path.toString());
        
        if (jarFileFormat) {
            sb.append("?jarFileFormat=true");
        }
        
        return new URL(sb.toString());
    }
}
