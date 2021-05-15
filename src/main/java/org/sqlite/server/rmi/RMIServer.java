/*
 * Copyright (c) 2021 little-pan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sqlite.server.rmi;

import org.sqlite.rmi.RMIDriver;
import org.sqlite.server.Config;
import org.sqlite.server.Server;
import org.sqlite.server.rmi.impl.RMIDriverImpl;

import java.io.File;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

public class RMIServer implements Server {
    static Logger log = Logger.getLogger(RMIServer.class.getSimpleName());

    protected final Config config;
    protected volatile Registry registry;
    private volatile boolean stopped;

    public RMIServer(Config config) {
        this.config = config;
    }

    @Override
    public void start()  throws IllegalStateException {
        if (this.stopped) {
            throw new IllegalStateException("Server stopped");
        }

        Config config = this.config;
        File baseDir = new File(config.getBaseDir());
        if (!baseDir.isDirectory() && !baseDir.mkdirs()) {
            String s = "Can't make base dir '" + baseDir + "'";
            throw new IllegalStateException(s);
        }
        File dataDir = new File(config.getDataDir());
        if (!dataDir.isDirectory() && !dataDir.mkdirs()) {
            String s = "Can't make data dir '" + dataDir + "'";
            throw new IllegalStateException(s);
        }

        int port = config.getPort();
        try {
            this.registry = LocateRegistry.createRegistry(port);
            RMIDriver driver = new RMIDriverImpl(config);
            this.registry.rebind(NAME, driver);
            String f = "%s v%s listen on %d";
            log.info(() -> String.format(f, NAME, VERSION, port));
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void stop() throws IllegalStateException {
        Registry registry = this.registry;
        if (registry != null) {
            try {
                registry.unbind(NAME);
            } catch (RemoteException e) {
                throw new IllegalStateException(e);
            } catch (NotBoundException ignore) {
                // Ignore
            }
        }
        this.stopped = true;
    }

    @Override
    public boolean isStopped() {
        return this.stopped;
    }

}