/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.module.*;
import java.util.concurrent.*;

/**
 * Provides a means of checking that events are received within a certain
 * period of time.
 */
public class EventChecker {
    private final long timeout;

    private final RepositoryListener repositoryListener;

    private final BlockingQueue<RepositoryEvent> initEventQueue =
            new LinkedBlockingQueue<RepositoryEvent>();
    private final BlockingQueue<RepositoryEvent> shutdownEventQueue =
            new LinkedBlockingQueue<RepositoryEvent>();
    private final BlockingQueue<RepositoryEvent> installEventQueue =
            new LinkedBlockingQueue<RepositoryEvent>();
    private final BlockingQueue<RepositoryEvent> uninstallEventQueue =
            new LinkedBlockingQueue<RepositoryEvent>();

    EventChecker() {
        this(100L);
    }

    EventChecker(long timeout) {
        this.timeout = timeout;

        repositoryListener = new RepositoryListener()  {
            public void handleEvent(RepositoryEvent e)  {
                if (e.getType() == RepositoryEvent.Type.REPOSITORY_INITIALIZED) {
                    initEventQueue.add(e);
                }
                if (e.getType() == RepositoryEvent.Type.REPOSITORY_SHUTDOWN) {
                    shutdownEventQueue.add(e);
                }
                if (e.getType() == RepositoryEvent.Type.MODULE_INSTALLED) {
                    installEventQueue.add(e);
                }
                if (e.getType() == RepositoryEvent.Type.MODULE_UNINSTALLED)  {
                    uninstallEventQueue.add(e);
                }
            }
        };
        Repository.addRepositoryListener(repositoryListener);
    }

    void clear() {
        // Hope 10 seconds is long enough for outstanding events to arrive.
        sleep(1000L * 10);

        initEventQueue.clear();
        shutdownEventQueue.clear();
        installEventQueue.clear();
        uninstallEventQueue.clear();
    }

    int getInstallEventQueueSize() {
        sleep(timeout);
        return installEventQueue.size();
    }

    int getUninstallEventQueueSize() {
        sleep(timeout);
        return uninstallEventQueue.size();
    }

    void end() {
        Repository.removeRepositoryListener(repositoryListener);
    }

    boolean initializeEventExists(Repository repo) throws Exception {
        return eventExists(initEventQueue, RepositoryEvent.Type.REPOSITORY_INITIALIZED, repo, null);
    }

    boolean shutdownEventExists(Repository repo) throws Exception {
        return eventExists(shutdownEventQueue, RepositoryEvent.Type.REPOSITORY_SHUTDOWN, repo, null);
    }

    boolean installEventExists(Repository repo, ModuleArchiveInfo mai) throws Exception {
        return eventExists(installEventQueue, RepositoryEvent.Type.MODULE_INSTALLED, repo, mai);
    }

    boolean uninstallEventExists(Repository repo, ModuleArchiveInfo mai) throws Exception {
        return eventExists(uninstallEventQueue, RepositoryEvent.Type.MODULE_UNINSTALLED, repo, mai);
    }

    boolean eventExists(
            BlockingQueue<RepositoryEvent> q,
            RepositoryEvent.Type t,
            Repository repo,
            ModuleArchiveInfo mai) throws Exception {

        RepositoryEvent evt = q.poll(timeout, TimeUnit.MILLISECONDS);
        if (evt != null) {
            if (evt.getType() != t) {
                throw new Exception("Expected event type: " + t + " but got " + evt.getType());
            }
            if (evt.getSource() != repo) {
                throw new Exception("Unexpected " + t + " event from: " + evt.getSource());
            }
            if (mai != null && evt.getModuleArchiveInfo() != mai) {
                throw new Exception("Unexpected " + t + " event for: " + evt.getModuleArchiveInfo());
            }
            return true;
        } else {
            return false;
        }
    }

    void sleep(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException ex) {
            throw new RuntimeException("clear interrupted");
        }
    }
}
