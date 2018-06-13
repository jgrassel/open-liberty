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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Nio2FileSystemURLConnection extends URLConnection {
    private boolean connected = false;
    private UUID uuid = null;
    private String pathString = null;
    private Path path = null;
    private boolean jarFileFormat = false;
    
    private Nio2FileSystemRegistryToken token = null;
    
    public Nio2FileSystemURLConnection(URL url) {
        super(url);       
    }

    @Override
    public void connect() throws IOException {
        if (!connected) {
            final String protocol = url.getProtocol();
            if (!Nio2FileSystemURLStreamHandlerFactory.PROTOCOL.equalsIgnoreCase(protocol)) {
                throw new IOException("Nio2FileSystemURLConnection cannot service protocol \"" + protocol + "\".");
            }
            
            // Resolve Nio2FileSystemRegistryToken
            try {
                uuid = UUID.fromString(url.getHost());
                token = Nio2StreamServiceRegistry.resolve(uuid);
                if (token == null || !token.isRegistered()) {
                    throw new IOException("Unregistered UUID: " + uuid);
                }
            } catch (IllegalArgumentException iae) {
                throw new IOException("Invalid UUID with URL.", iae);
            }
            
            pathString = url.getPath();
            try {
                if (pathString == null || pathString.isEmpty()) {
                    // Default to root if not specified in the URL
                    pathString = "/";
                }
                path = token.getFileSystem().getPath(pathString);
            } catch (Exception e) {
                throw new IOException(e);
            }
                        
            String query = url.getQuery();
            if (query != null) {
                String[] queries = query.split(",");
                for (String q : queries) {
                    if ("jarFileFormat=true".equals(q)) {
                        jarFileFormat = true;
                    }
                }
            }
    
            connected = true;
        }
    }

    public InputStream getInputStream() throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Path does not exist.");
        }
        
        if (jarFileFormat) {
            return new JarFormatInputStream(path);
        } else {
            if (Files.isRegularFile(path)) {
                // Is regular file
                return Files.newInputStream(path);
            } else {
                // Directory
                // TODO: Determine whether to fail, or try come up with a directory list in some standard format
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Nio2FileSystemURLConnection [connected=" + connected + ", uuid=" + uuid + ", path=" + path
                + ", zipFileFormat=" + jarFileFormat + "]";
    }
    
    private class JarFormatInputStream extends InputStream {
        private Path streamRoot;
        private List<Path> filesList = new ArrayList<Path>();
        
        private final Iterator<? extends Path> entries;
        
        /**
         * The input stream for the current entry, or null if there is no current
         * entry.
         */
        private InputStream entryInput;
        
        /**
         * The output stream used to write zip entries. This ZipOutputStream
         * writes directly to {@link #buffer}.
         */
        private final ZipOutputStream out = new ZipOutputStream(new InternalBufferOutputStream());
        
        /**
         * The temporary buffer for reading input from a zip entry.
         */
        private final byte[] entryInputBuffer = new byte[8192];
        
        /**
         * The buffer for this input stream. This is the input buffer for this,
         * but the output buffer for {@link #out}.
         */
        byte[] buffer = new byte[8192];
        
        /**
         * The position of the next byte to read from {@link #buffer}, with an
         * exclusive upper bound of {@link #max}.
         */
        int pos;

        /**
         * The maximum number of valid bytes in {@link #buffer}.
         */
        int max;
        
        private JarFormatInputStream(Path streamRoot) throws IOException {
            Objects.requireNonNull(streamRoot);
            this.streamRoot = streamRoot;
            catalogZipEntries();
            entries = filesList.iterator();
        }
        
        @Override
        public int read() throws IOException {
            if (pos == max && !refill()) {
                return -1;
            }
            return buffer[pos++] & 0xff;
        }
        
        @Override
        public void close() throws IOException {
            if (entryInput != null) {
                entryInput.close();
            }
            
            // TODO : CLose rest
        }
        
        private void catalogZipEntries() throws IOException {
            Files.walkFileTree(streamRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    filesList.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        
        private boolean refill() throws IOException {
            pos = max = 0;

            // Write as many entries as possible until ZipOutputStream flushes
            // some data to our internal buffer (via BufferOutputStream), and
            // then return that data to the caller.

            for (;;) {
                // First, copy any remaining input from the open entry.
                if (entryInput != null) {
                    for (;;) {
                        int read;
                        if ((read = entryInput.read(entryInputBuffer, 0, entryInputBuffer.length)) == -1) {
                            entryInput.close();
                            entryInput = null;

                            out.closeEntry();
                            if (max != 0) {
                                return true;
                            }

                            // Try the next entry.
                            break;
                        }

                        out.write(entryInputBuffer, 0, read);
                        if (max != 0) {
                            return true;
                        }
                    }
                }

                // Otherwise, try to find a new entry that matches.
                for (;;) {
                    if (!entries.hasNext()) {
                        out.close();
                        // Closing the ZipOutputStream probably writes trailing bytes.
                        return max != 0;
                    }

                    Path entry = entries.next();
                    entryInput = Files.newInputStream(entry);
                    out.putNextEntry(new ZipEntry(entry.getFileName().toString()));
                    if (max != 0) {
                        return true;
                    }
                    
                    break;
                }
            }
        }
        
        private class InternalBufferOutputStream extends OutputStream {
            @Override
            public void write(int b) throws IOException {
                if (max == buffer.length) {
                    // We must grow the buffer as needed by the ZipOutputStream.
                    buffer = Arrays.copyOf(buffer, max + max);
                }

                buffer[max++] = (byte) b;
            }
        }
    }
}
