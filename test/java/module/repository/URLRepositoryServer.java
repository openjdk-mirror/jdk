/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.net.httpserver.*;

/**
 * Creates an HTTP server for testing URLRepositories.
 */
public class URLRepositoryServer {
    static final boolean debug = System.getProperty("repository.debug") != null;

    private final File base;

    private final URL url;

    private final HttpServer server;

    /**
     * Creates an HTTP server rooted at {@code base}.
     */
    public URLRepositoryServer(File base) throws IOException {
        this.base = base;
        server= HttpServer.create(
            new InetSocketAddress((InetAddress) null, 0), 0);
        url = new URL("http://localhost:"
            + new Integer(server.getAddress().getPort()).toString());
    }

    public URL getURL() {
        return url;
    }

    public boolean start() throws Throwable {
        boolean rc = false;

        HttpContext context = server.createContext("/",
            new HttpHandler() {
            public void handle(HttpExchange e) throws IOException {
                try {
                    FileInputStream fis = null;
                    String uri = e.getRequestURI().toString();
                    File served = new File(base, uri);
                    if (!served.exists()) {
                        if (debug) System.err.println(
                            "URLRepositoryServer: No such file " +  uri);
                    } else {
                        if (debug) System.err.println(
                            "URLRepositoryServer: Serving file " +  uri);
                        fis = new FileInputStream(served);

                        Headers h = e.getResponseHeaders();
                        List tmp = new ArrayList<String>();
                        SimpleDateFormat fo =
                            new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                        fo.setTimeZone(TimeZone.getTimeZone("GMT"));
                        h.set("last-modified", fo.format(served.lastModified()));
                        e.sendResponseHeaders(200, served.length());
                        OutputStream os = e.getResponseBody();
                        byte[] buf = new byte[1024];
                        int count = 0;
                        while ((count = fis.read(buf)) != -1) {
                            os.write(buf, 0, count);
                        }
                        fis.close();
                        e.close();
                    }
                } catch (Throwable ex) {
                    server.stop(0);
                    throw new IOException(ex);
                } finally {
                    e.close();
                }
            }
        });

        try {
            server.start();
            rc = true;
        } catch (Throwable t) {
            server.stop(0);
            throw t;
        }

        return rc;
    }

    public void stop() {
        server.stop(0);
    }
}
